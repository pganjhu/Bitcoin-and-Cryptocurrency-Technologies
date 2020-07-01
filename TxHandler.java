import java.util.ArrayList;

public class TxHandler {

    private UTXOPool _utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        _utxoPool = new UTXOPool(utxoPool);
    }

    public boolean isValidTx(Transaction tx) {
        double sumInput = 0;
        double sumOutput = 0;
        ArrayList<UTXO> usedUTXO = new ArrayList<>();

        for (int i=0;i<tx.numInputs();i++) {
            Transaction.Input input = tx.getInput(i);
            int outputIndex = input.outputIndex;
            byte[] prevTxHash = input.prevTxHash;
            byte[] signature = input.signature;

            UTXO utxo = new UTXO(prevTxHash, outputIndex);

            if (!_utxoPool.contains(utxo)) {
                return false;
            }
            Transaction.Output output = _utxoPool.getTxOutput(utxo);
            byte[] message = tx.getRawDataToSign(i);
            if (!Crypto.verifySignature(output.address,message,signature)) {
                return false;
            }
            if (usedUTXO.contains(utxo)) {
                return false;
            }
            usedUTXO.add(utxo);
            sumInput += output.value;
        }
        for (int i=0;i<tx.numOutputs();i++) {
            Transaction.Output output = tx.getOutput(i);
            if (output.value < 0) {
                return false;
            }
            sumOutput += output.value;
        }
        if (sumInput < sumOutput) {
            return false;
        }
        return true;
    }

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<>();
        for (Transaction t : possibleTxs) {
            if (isValidTx(t)) {
                validTxs.add(t);

                //remove utxo
                for (Transaction.Input input : t.getInputs()) {
                    int outputIndex = input.outputIndex;
                    byte[] prevTxHash = input.prevTxHash;
                    UTXO utxo = new UTXO(prevTxHash, outputIndex);
                    _utxoPool.removeUTXO(utxo);
                }
                //add new utxo
                byte[] hash = t.getHash();
                for (int i=0;i<t.numOutputs();i++) {
                    UTXO utxo = new UTXO(hash, i);
                    _utxoPool.addUTXO(utxo, t.getOutput(i));
                }
            }
        }
        Transaction[] validTxsArr = new Transaction[validTxs.size()];
        validTxsArr = validTxs.toArray(validTxsArr);
        return validTxsArr;
    }

}