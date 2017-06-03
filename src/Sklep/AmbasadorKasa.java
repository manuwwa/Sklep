package Sklep;

import hla.rti1516e.NullFederateAmbassador;

/**
 * Created by Marcin on 03.06.2017.
 */
public class AmbasadorKasa extends NullFederateAmbassador {

    FederatKasa federate;
    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    //----------------------------------------------------------
    //                      CONSTRUCTORS
    //----------------------------------------------------------
    public AmbasadorKasa (FederatKasa federate)
    {
        this.federate=federate;
    }
    //----------------------------------------------------------
    //                    INSTANCE METHODS
    //----------------------------------------------------------
    private void log( String message )
    {
        System.out.println( "FederateAmbassador: " + message );
    }

}
