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
    public void initCrypto() {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048, random);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testValidTransaction() {
        pool = new UTXOPool();

        KeyPair pair = keyGen.generateKeyPair();

        Transaction rt = createTransaction("root transaction");
        rt.addOutput(42, pair.getPublic());
        UTXO utxo = new UTXO(rt.getHash(), 0);
        pool.addUTXO(utxo, rt.getOutput(0));


        Transaction t;

        try {
            t = createTransaction("test transaction");
            t.addInput(rt.getHash(), 0);
            t.addOutput(21, pair.getPublic());
            t.addOutput(21, pair.getPublic());
            t.addSignature(sign(pair.getPrivate(), t.getRawDataToSign(0)), 0);
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }

        handler = new TxHandler(pool);
        Assert.assertTrue(handler.isValidTx(t));
    }

    @Test
    public void testInvalidTransactionMoreOutput() {
        pool = new UTXOPool();

        KeyPair pair = keyGen.generateKeyPair();

        Transaction rt = createTransaction("root transaction");
        rt.addOutput(42, pair.getPublic());
        UTXO utxo = new UTXO(rt.getHash(), 0);
        pool.addUTXO(utxo, rt.getOutput(0));


        Transaction t;

        try {
            t = createTransaction("test transaction");
            t.addInput(rt.getHash(), 0);
            t.addOutput(21, pair.getPublic());
            t.addOutput(23, pair.getPublic());
            t.addSignature(sign(pair.getPrivate(), t.getRawDataToSign(0)), 0);
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }

        handler = new TxHandler(pool);
        Assert.assertFalse(handler.isValidTx(t));
    }

    private byte[] sign(PrivateKey privateKey, byte[] rawDataToSign) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(rawDataToSign);
        return sig.sign();
    }

    private Transaction createTransaction(String name) {
        Transaction t = new Transaction();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(name.getBytes());

            t.setHash(md.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return t;
    }
}
