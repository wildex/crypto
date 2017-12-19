package tests;

import com.examples.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;

import java.security.*;

public class TxHandlerTest {
    private UTXOPool pool;
    private TxHandler handler;
    private ArrayList<Transaction> transactionLedger;

    private KeyPairGenerator keyGen;

    @Before
    public void initLedgers() {
        pool = new UTXOPool();
        handler = new TxHandler(pool);
    }

    @Before
    public void initCrypto() {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
            keyGen.initialize(2048, random);
        } catch (Exception e) {}

    }

    @Before
    public void dummyData() {
        try {
            MessageDigest md = MessageDigest.getInstance( "SHA-256" );
            KeyPair pair = keyGen.generateKeyPair();

            Transaction t = new Transaction();
            t.addOutput(42, pair.getPublic());
            md.update("utxo transaction".getBytes());
            t.setHash(md.digest());
            transactionLedger.add(t);

            for (int i = 1; i < 10; i++) {
                md.update(String.format("%d test transaction", i).getBytes());
                pair = keyGen.generateKeyPair();
                t = new Transaction();
                t.setHash(md.digest());

                t.addOutput(10, pair.getPublic());

                t.addInput(transactionLedger.get(i - 1).getHash(), 0);
                transactionLedger.add(t);
            }


            UTXO utxo = new UTXO(md.digest(), 0);
            pool.addUTXO(utxo, t.getOutput(0));
        } catch (Exception e) {}
    }

    @Test
    public void testValidateTransaction() {
        handler.handleTxs(transactionLedger.toArray(new Transaction[transactionLedger.size()]));
    }
}
