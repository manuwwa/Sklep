package Sklep;

/**
 * Created by Marcin on 03.06.2017.
 */
public class FederatKasa {
    /**
     * This is just a helper method to make sure all logging it output in the same form
     */
    private void log( String message )
    {
        System.out.println( "ExampleFederate   : " + message );
    }
    public void runFederate( String federateName ) throws Exception {

    }

    public static void main( String[] args )
    {
        String federateName = "exampleFederate";

        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            // run the example federate
            FederatKasa kasa=   new FederatKasa();
                 kasa.runFederate( federateName );
        }
        catch( Exception rtie )
        {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}
