import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

public class Zip {
    private enum Action {
        COMPRESS,
        DECOMPRESS;

        public static Action getAction(String action) {
            switch (action) {
                case "compression":
                    return COMPRESS;

                case "decompression":
                    return DECOMPRESS;

                default:
                    return null;
            }
        }
    }

    Config config;
    private String inputFile;
    private String outputFile;
    private Action action;
    private int bufferSize;
    private final byte BYTE_SIZE = 8;

    Zip(Config config) {
        this.config = config;
    }

    private Error parseConfig() {
        inputFile = config.getArg(Config.Configuration.INPUT_FILE);

        action = Action.getAction(config.getArg(Config.Configuration.ACTION));
        if (action == null) {
            return Error.NOT_CORRECT_ACTION;
        }

        if (!config.getArg(Config.Configuration.OUTPUT_FILE).equals("")) {
            outputFile = config.getArg(Config.Configuration.OUTPUT_FILE);
        }

        try {
            bufferSize = Integer.parseInt(config.getArg(Config.Configuration.BUFFER_SIZE));
            if (bufferSize < 0) {
                return Error.NOT_CORRECT_BUFFER_SIZE;
            }
        } catch (NumberFormatException exception) {
            return Error.NOT_CORRECT_BUFFER_SIZE;
        }

        return null;
    }

    private Error compress() {
        Compress compress = new Compress();

        try (FileInputStream fileInputStream = new FileInputStream(inputFile)) {
            int available = fileInputStream.available();
            int readSize = Math.min(available, bufferSize);
            while (available > 0) {
                byte[] bytes = new byte[readSize];

                if (fileInputStream.read(bytes) == -1) {
                    return Error.READ_ERROR;
                }

                compress.fillFrequencyTable(bytes);

                available -= readSize;
                readSize = Math.min(available, bufferSize);
            }

        } catch (IOException ex) {
            return Error.COULD_NOT_OPEN_FILE;
        }

        compress.createHuffmanTree();
        compress.createCodingTable();

        System.out.println("Code table created\n" +
                "Compress started");
        try {
            FileInputStream fileInputStream = new FileInputStream(inputFile);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

            String tree = compress.getTreeText();
            String treeSize = String.format("%16s", Integer.toBinaryString(tree.length())).replace(" ", "0");
            StringBuilder res = new StringBuilder(treeSize + tree);

            int available = fileInputStream.available();
            int completePercent = available;
            int readSize = Math.min(available, bufferSize);

            while (available > 0) {
                if (res.length() >= readSize * BYTE_SIZE) {
                    String tmp = res.substring(0, BYTE_SIZE * readSize);
                    res = new StringBuilder(res.substring(BYTE_SIZE * readSize));

                    byte[] compressedBytes = new byte[readSize];
                    for (int i = 0; i < readSize; i++) {
                        compressedBytes[i] = (byte) Integer.parseInt(tmp.substring(i * BYTE_SIZE, (i + 1) * BYTE_SIZE), 2);
                    }
                    fileOutputStream.write(compressedBytes);
                }

                byte[] bytes = new byte[readSize];
                if (fileInputStream.read(bytes) == -1) {
                    return Error.READ_ERROR;
                }

                for (byte b : bytes) {
                    res.append(compress.getCompressedByte(b));
                }

                available -= readSize;
                if (available == 0)
                    break;
                readSize = Math.min(available, bufferSize);
            }

            int counter = 0;
            if (res.length() % BYTE_SIZE != 0)
                counter = BYTE_SIZE - res.length() % BYTE_SIZE;
            res.append(String.join("", Collections.nCopies(counter, "0")));
            res.append(String.format("%8s", Integer.toBinaryString(counter & compress.MASK)).replace(" ", "0"));

            byte[] compressedBytes = new byte[res.length() / BYTE_SIZE];
            for (int i = 0; i < res.length() / BYTE_SIZE; i++) {
                compressedBytes[i] = (byte) Integer.parseInt(res.substring(i * BYTE_SIZE, (i + 1) * BYTE_SIZE), 2);
            }
            fileOutputStream.write(compressedBytes);

            fileInputStream.close();
            fileOutputStream.close();
        } catch (IOException exception) {
            return Error.COULD_NOT_OPEN_FILE;
        }

        return null;
    }

    private Error decompress() {
        try {
            FileInputStream fileInputStream = new FileInputStream(inputFile);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

            byte[] byteTreeSize = new byte[2];
            if (fileInputStream.read(byteTreeSize) == -1) {
                return Error.READ_ERROR;
            }

            String treeSizeText = String.format("%8s", Integer.toBinaryString(byteTreeSize[0] & 0xff)).replace(" ", "0")
                    + String.format("%8s", Integer.toBinaryString(byteTreeSize[1] & 0xff)).replace(" ", "0");

            int treeSize = Integer.parseInt(treeSizeText, 2);

            StringBuilder res = new StringBuilder();

            byte[] treeBytes = new byte[treeSize / BYTE_SIZE + 1];
            if (fileInputStream.read(treeBytes) == -1) {
                return Error.READ_ERROR;
            }

            for (byte b : treeBytes) {
                res.append(String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(" ", "0"));
            }

            String treeText = res.substring(0, treeSize);
            res = new StringBuilder(res.substring(treeSize));
            Decompress decompress = new Decompress(treeText);
            decompress.createHuffmanTree();
            decompress.createCodingTable();
            decompress.fillHufTable();

            int available = fileInputStream.available();
            int completePercent = available;
            int readSize = Math.min(available, bufferSize);
            while (available > 0) {
                byte[] bytes = new byte[readSize];
                if (fileInputStream.read(bytes) == -1) {
                    return Error.READ_ERROR;
                }

                for (byte b : bytes) {
                    res.append(String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(" ", "0"));
                }

                available -= readSize;
                if (res.length() >= bufferSize * BYTE_SIZE || available <= 0) {
                    String character = "";
                    byte[] tmp = new byte[res.length()];
                    int tmpIndex = 0;
                    while (res.length() != 0 && res.length() > BYTE_SIZE * 2) {
                        character += res.charAt(0);
                        String decChar = decompress.getChar(character);
                        if (decChar != null) {
                            tmp[tmpIndex] = (byte) Integer.parseInt(decChar, 2);
                            tmpIndex++;
                            character = "";
                        }
                        res = new StringBuilder(res.substring(1));
                    }

                    fileOutputStream.write(tmp, 0, tmpIndex);
                }

                if (available <= 0)
                    break;
                readSize = Math.min(available, bufferSize);
            }

            int countZero = Integer.parseInt(res.substring(BYTE_SIZE, BYTE_SIZE * 2), 2);
            res = new StringBuilder(res.substring(0, res.length() - countZero - BYTE_SIZE));
            String character = "";
            while (res.length() != 0) {
                character += res.charAt(0);
                String decChar = decompress.getChar(character);
                if (decChar != null) {
                    fileOutputStream.write((byte) Integer.parseInt(decChar, 2));
                    character = "";
                }
                res = new StringBuilder(res.substring(1));
            }

            fileInputStream.close();
            fileOutputStream.close();
        } catch (IOException exception) {
            return Error.COULD_NOT_OPEN_FILE;
        }

        return null;
    }

    public Error run() {
        Error error = parseConfig();
        if (error != null)
            return error;

        if (action == Action.COMPRESS) {
            error = compress();
        } else {
            error = decompress();
        }

        return error;
    }
}
