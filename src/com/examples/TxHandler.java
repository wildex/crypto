package com.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TxHandler {

    private UTXOPool ledger;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        ledger = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        HashMap<UTXO, Integer> claimedUTXOs = new HashMap<UTXO, Integer>();

        double inputSum = 0;
        double outputSum = 0;
        // all outputs claimed by {@code tx} are in the current UTXO pool
        // the signatures on each input of {@code tx} are valid
        // no UTXO is claimed multiple times by {@code tx}
        for (int i = 0; i < txInputs.size(); i++) {
            UTXO utxo = new UTXO(txInputs.get(i).prevTxHash, txInputs.get(i).outputIndex);

            Transaction.Output output = ledger.getTxOutput(utxo);
            if (output == null) {
                return false;
            }

            if (claimedUTXOs.get(utxo) != null) {
                return false;
            }
            claimedUTXOs.put(utxo, 1);

            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), txInputs.get(i).signature)) {
                return false;
            }

            inputSum += output.value;
        }

        // all of {@code tx}s output values are non-negative
        for (Transaction.Output txOutput : txOutputs) {
            if (txOutput.value < 0) {
                return false;
            }

            outputSum += txOutput.value;
        }

        // the sum of {@code tx}s input values is greater than or equal to the sum of its output
        return inputSum >= outputSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTransactions = new ArrayList<Transaction>();
        HashMap<Transaction.Output, Integer> spentOutputs = new HashMap<Transaction.Output, Integer>();

        for (Transaction t: possibleTxs) {
            if (isValidTx(t)) {
                for (Transaction.Input in: t.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

                    Transaction.Output output = ledger.getTxOutput(utxo);
                    ledger.removeUTXO(utxo);

                    for (Transaction pt: possibleTxs) {
                        if (Arrays.equals(pt.getHash(), in.prevTxHash) && pt.getOutput(in.outputIndex) != null) {
                            output = pt.getOutput(in.outputIndex);
                        }
                    }

                    if (output == null) {
                        continue;
                    }

                    if (spentOutputs.get(output) != null) {
                        continue;
                    }

                    spentOutputs.put(output, 1);
                }

                // add transaction outputs to ledger
                for (int i = 0; i < t.getOutputs().size(); i++) {
                    if (spentOutputs.get(t.getOutputs().get(i)) != null) {
                        continue;
                    }

                    UTXO utxo = new UTXO(t.getHash(), i);
                    if (!ledger.contains(utxo)) {
                        ledger.addUTXO(utxo, t.getOutputs().get(i));
                    }
                }
                validTransactions.add(t);
            }
        }

        return validTransactions.toArray(new Transaction[validTransactions.size()]);
    }
}
