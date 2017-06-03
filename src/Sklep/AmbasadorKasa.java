package Sklep;

import hla.rti1516e.NullFederateAmbassador;

/**
 * Created by Marcin on 03.06.2017.
 */
public class AmbasadorKasa extends NullFederateAmbassador {

    FederatKasa federate;


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
