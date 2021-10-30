public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Give me config file");
            return;
        }
        if (args.length > 1){
            System.out.println("Too many arguments! Give me only config file");
            return;
        }

        Config config = new Config();

        Error error;

        error = config.parse(args[0]);

        if (error != null) {
            System.out.println(error.getErrorMessage());
            return;
        }

        Zip zip = new Zip(config);

        error = zip.run();

        if (error != null) {
            System.out.println(error.getErrorMessage());
            return;
        }

        System.out.println("Complete");
    }
}
