package Sklep;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.time.HLAfloat64TimeFactory;

/**
 * Created by Marcin on 03.06.2017.
 */
public class FederatKasa {
    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private RTIambassador rtiamb;
    private ExampleFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    // caches of handle types - set once we join a federation
    protected ObjectClassHandle sodaHandle;
    protected AttributeHandle cupsHandle;
    protected AttributeHandle flavHandle;
    protected InteractionClassHandle servedHandle;


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
