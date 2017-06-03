package Sklep;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

/**
 * Created by Marcin on 03.06.2017.
 */
public class FederatKasa  {
    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    public static final String READY_TO_RUN = "ReadyToRun";
    private RTIambassador rtiamb;
    private AmbasadorKasa fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    // caches of handle types - set once we join a federation
    protected ObjectClassHandle KasaHandle;
    protected AttributeHandle NumerKasy;
    protected AttributeHandle Dlugosc;
    protected AttributeHandle CzyPelna;
    protected AttributeHandle CzyOtwarta;
    protected InteractionClassHandle servedHandle;
    protected ObjectClassHandle KlientHandle;
    protected AttributeHandle IDKlienta ;
    protected AttributeHandle Uprzywilejowany;
    protected AttributeHandle NumerKolejki ;
    protected AttributeHandle NumerWKolejce ;
    protected AttributeHandle Gotowka ;
    public static final int ITERATIONS = 20;
    /**
     * This is just a helper method to make sure all logging it output in the same form
     */
    private void log( String message )
    {
        System.out.println( "ExampleFederate   : " + message );
    }
    public void runFederate( String federateName ) throws Exception {
        log( "Creating RTIambassador" );
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        // connect
        log( "Connecting..." );
        fedamb = new AmbasadorKasa(this);
        rtiamb.connect( fedamb, CallbackModel.HLA_EVOKED );
        //////////////////////////////
        // 3. create the federation //
        //////////////////////////////
        log( "Creating Federation..." );
        try
        {
            URL[] modules = new URL[]{
                    (new File("foms/ModelFom.xml").toURI().toURL())
            };

            rtiamb.createFederationExecution( "ExampleFederation", modules );
            log( "Created Federation" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Didn't create federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            log( "Exception loading one of the FOM modules from disk: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }
        ////////////////////////////
        // 4. join the federation //
        ////////////////////////////
        URL[] joinModules = new URL[]{
                (new File("foms/ModelFom.xml").toURI().toURL())
        };


        rtiamb.joinFederationExecution( federateName,            // name for the federate
                "ExampleFederateType",   // federate type
                "ExampleFederation",     // name of federation
                joinModules );           // modules we want to add

        log( "Joined Federation as " + federateName );

        // cache the time factory for easy access
        this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();
        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        // wait until the point is announced
        while( fedamb.isAnnounced == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
        waitForUser();

        ///////////////////////////////////////////////////////
        // 6. achieve the point and wait for synchronization //
        ///////////////////////////////////////////////////////
        // tell the RTI we are ready to move past the sync point and then wait
        // until the federation has synchronized on
        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( fedamb.isReadyToRun == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
        /////////////////////////////
        // 7. enable time policies //
        /////////////////////////////
        // in this section we enable/disable all time policies
        // note that this step is optional!
        enableTimePolicy();
        log( "Time Policy Enabled" );



        //////////////////////////////
        // 8. publish and subscribe //
        //////////////////////////////
        // in this section we tell the RTI of all the data we are going to
        // produce, and all the data we want to know about
        publishAndSubscribe();
        log( "Published and Subscribed" );

        /////////////////////////////////////
        // 9. register an object to update //
        /////////////////////////////////////
        ObjectInstanceHandle objectHandle = registerObject();
        log( "Registered Object, handle=" + objectHandle );

        /////////////////////////////////////
        // 10. do the main simulation loop //
        /////////////////////////////////////
        // here is where we do the meat of our work. in each iteration, we will
        // update the attribute values of the object we registered, and will
        // send an interaction.
        for( int i = 0; i < ITERATIONS; i++ )
        {
            // 9.1 update the attribute values of the instance //
            updateAttributeValues( objectHandle );

            // 9.2 send an interaction
           // sendInteraction();

            // 9.3 request a time advance and wait until we get it
            advanceTime( 1.0 );
            log( "Time Advanced to " + fedamb.federateTime );
        }

        //////////////////////////////////////
        // 11. delete the object we created //
        //////////////////////////////////////
        deleteObject( objectHandle );
        log( "Deleted Object, handle=" + objectHandle );

        ////////////////////////////////////
        // 12. resign from the federation //
        ////////////////////////////////////
        rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
        log( "Resigned from Federation" );

        ////////////////////////////////////////
        // 13. try and destroy the federation //
        ////////////////////////////////////////
        // NOTE: we won't die if we can't do this because other federates
        //       remain. in that case we'll leave it for them to clean up
        try
        {
            rtiamb.destroyFederationExecution( "ExampleFederation" );
            log( "Destroyed Federation" );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
            log( "No need to destroy federation, it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
            log( "Didn't destroy federation, federates still joined" );
        }

    }
    private ObjectInstanceHandle registerObject() throws RTIexception
    {
        return rtiamb.registerObjectInstance( KasaHandle );
    }
    /**
     * This method will block until the user presses enter
     */
    private void waitForUser()
    {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
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
    private void deleteObject( ObjectInstanceHandle handle ) throws RTIexception
    {
        rtiamb.deleteObjectInstance( handle, generateTag() );
    }

    /**
     * This method will inform the RTI about the types of data that the federate will
     * be creating, and the types of data we are interested in hearing about as other
     * federates produce it.
     */
    private void publishAndSubscribe() throws RTIexception
    {
        ///////////////////////////////////////////////
        // publish all attributes of Food.Drink.Soda //
        ///////////////////////////////////////////////
        // before we can register instance of the object class Food.Drink.Soda and
        // update the values of the various attributes, we need to tell the RTI
        // that we intend to publish this information

        // get all the handle information for the attributes of Food.Drink.Soda
        this.KasaHandle = rtiamb.getObjectClassHandle( "HLAobjectRoot.Kasa" );
        this.NumerKasy = rtiamb.getAttributeHandle(KasaHandle, "NumerKasy" );
        this.Dlugosc = rtiamb.getAttributeHandle(KasaHandle, "Dlugosc" );
        this.CzyPelna = rtiamb.getAttributeHandle(KasaHandle, "CzyPelna" );
        this.CzyOtwarta = rtiamb.getAttributeHandle(KasaHandle, "CzyOtwarta" );
        // package the information into a handle set
        AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
        attributes.add(NumerKasy);
        attributes.add(Dlugosc);
        attributes.add(CzyPelna);
        attributes.add(CzyOtwarta);
        // do the actual publication
        rtiamb.publishObjectClassAttributes(KasaHandle, attributes );

        ////////////////////////////////////////////////////
        // subscribe to all attributes of Food.Drink.Soda //
        ////////////////////////////////////////////////////
        // we also want to hear about the same sort of information as it is
        // created and altered in other federates, so we need to subscribe to it
        this.KlientHandle = rtiamb.getObjectClassHandle( "HLAobjectRoot.Klient" );
        this.IDKlienta = rtiamb.getAttributeHandle(KasaHandle, "IDKlienta" );
        this.Uprzywilejowany = rtiamb.getAttributeHandle(KasaHandle, "Uprzywilejowany" );
        this.NumerKolejki = rtiamb.getAttributeHandle(KasaHandle, "NumerKolejki" );
        this.NumerWKolejce = rtiamb.getAttributeHandle(KasaHandle, "NumerWKolejce" );
        this.Gotowka = rtiamb.getAttributeHandle(KasaHandle, "Gotowka" );
        //AttributeHandleSet attributes
        attributes = rtiamb.getAttributeHandleSetFactory().create();
        attributes.add(IDKlienta);
        attributes.add(Uprzywilejowany);
        attributes.add(NumerKolejki);
        attributes.add(NumerWKolejce);
        attributes.add(Gotowka);
        rtiamb.subscribeObjectClassAttributes(KasaHandle, attributes );

        //////////////////////////////////////////////////////////
        // publish the interaction class FoodServed.DrinkServed //
        //////////////////////////////////////////////////////////
        // we want to send interactions of type FoodServed.DrinkServed, so we need
        // to tell the RTI that we're publishing it first. We don't need to
        // inform it of the parameters, only the class, making it much simpler
//        String iname = "HLAinteractionRoot.CustomerTransactions.FoodServed.DrinkServed";
//        servedHandle = rtiamb.getInteractionClassHandle( iname );

        // do the publication
//        rtiamb.publishInteractionClass( servedHandle );

        /////////////////////////////////////////////////////////
        // subscribe to the FoodServed.DrinkServed interaction //
        /////////////////////////////////////////////////////////
        // we also want to receive other interaction of the same type that are
        // sent out by other federates, so we have to subscribe to it first
        String iname = "HLAinteractionRoot.RozpoczecieObslugi";
        servedHandle = rtiamb.getInteractionClassHandle( iname );

        rtiamb.subscribeInteractionClass( servedHandle);
    }
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helper Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * This method will attempt to enable the various time related properties for
     * the federate
     */
    private void enableTimePolicy() throws Exception
    {
        // NOTE: Unfortunately, the LogicalTime/LogicalTimeInterval create code is
        //       Portico specific. You will have to alter this if you move to a
        //       different RTI implementation. As such, we've isolated it into a
        //       method so that any change only needs to happen in a couple of spots
        HLAfloat64Interval lookahead = timeFactory.makeInterval( fedamb.federateLookahead );

        ////////////////////////////
        // enable time regulation //
        ////////////////////////////
        this.rtiamb.enableTimeRegulation( lookahead );

        // tick until we get the callback
        while( fedamb.isRegulating == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        /////////////////////////////
        // enable time constrained //
        /////////////////////////////
        this.rtiamb.enableTimeConstrained();

        // tick until we get the callback
        while( fedamb.isConstrained == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }
    private void updateAttributeValues( ObjectInstanceHandle objectHandle ) throws RTIexception
    {
        ///////////////////////////////////////////////
        // create the necessary container and values //
        ///////////////////////////////////////////////
        // create a new map with an initial capacity - this will grow as required
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(2);

        // create the collection to store the values in, as you can see
        // this is quite a lot of work. You don't have to use the encoding
        // helpers if you don't want. The RTI just wants an arbitrary byte[]

        // generate the value for the number of cups (same as the timestep)
        HLAinteger16BE cupsValue = encoderFactory.createHLAinteger16BE( (short)16 );
        attributes.put(Dlugosc , cupsValue.toByteArray() );

        // generate the value for the flavour on our magically flavour changing drink
        // the values for the enum are defined in the FOM
        int randomValue = 101 + new Random().nextInt(3);
        HLAinteger32BE flavValue = encoderFactory.createHLAinteger32BE( randomValue );
        attributes.put( NumerKasy, flavValue.toByteArray() );

        //////////////////////////
        // do the actual update //
        //////////////////////////
        rtiamb.updateAttributeValues( objectHandle, attributes, generateTag() );

        // note that if you want to associate a particular timestamp with the
        // update. here we send another update, this time with a timestamp:
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );
        rtiamb.updateAttributeValues( objectHandle, attributes, generateTag(), time );
    }
    private byte[] generateTag()
    {
        return ("(timestamp) "+System.currentTimeMillis()).getBytes();
    }
    private void advanceTime( double timestep ) throws RTIexception
    {
        // request the advance
        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( time );

        // wait for the time advance to be granted. ticking will tell the
        // LRC to start delivering callbacks to the federate
        while( fedamb.isAdvancing )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }
}
