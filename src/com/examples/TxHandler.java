package com.examples;

import java.util.ArrayList;
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
        HashMap<Transaction.Output, Integer> claimedOutputs = new HashMap<Transaction.Output, Integer>();

        int inputSum = 0;
        // all outputs claimed by {@code tx} are in the current UTXO pool
        // the signatures on each input of {@code tx} are valid
        // no UTXO is claimed multiple times by {@code tx}
        for (int i = 0; i < txInputs.size(); i++) {
            Transaction.Output output = findCorrespondingOutput(txInputs.get(i));
            if (output == null) {
                return false;
            }
            if (claimedOutputs.get(output) != null) {
                return false;
            }
            claimedOutputs.put(output, 1);

            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), txInputs.get(i).signature)) {
                return false;
            }

            inputSum += output.value;
        }

        int outputSum = 0;
        // all of {@code tx}s output values are non-negative
        for (int i = 0; i < txOutputs.size(); i++) {
            if (txOutputs.get(i).value < 0) {
                return false;
            }

            outputSum += txOutputs.get(i).value;
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
                    Transaction.Output output = findCorrespondingOutput(in);
                    if (spentOutputs.get(output) != null) {
                        continue;
                    }

                    spentOutputs.put(output, 1);
                }
                // todo: clean up already spent outputs

                // add transaction outputs to ledger
                for (int i = 0; i < t.getOutputs().size(); i++) {
                    UTXO utxo = new UTXO(t.getHash(), i);
                    ledger.addUTXO(utxo, t.getOutputs().get(i));
                }
                validTransactions.add(t);
            }
        }

        return (Transaction[]) validTransactions.toArray();
    }


    private Transaction.Output findCorrespondingOutput(Transaction.Input input) {
        for (UTXO utxo: ledger.getAllUTXO()) {
            if (
                utxo.getTxHash() == input.prevTxHash
                        && utxo.getIndex() == input.outputIndex
            ) {
                return ledger.getTxOutput(utxo);
            }
        }

        return null;
    }
}
