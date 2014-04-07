package no.difi.oppslagstjenesten.yt;

import no.difi.begrep.Person;
import no.difi.kontaktinfo.external.client.cxf.WSS4JInterceptorHelper;
import no.difi.kontaktinfo.wsdl.oppslagstjeneste_14_05.Oppslagstjeneste1405;
import no.difi.kontaktinfo.xsd.oppslagstjeneste._14_05.HentEndringerForespoersel;
import no.difi.kontaktinfo.xsd.oppslagstjeneste._14_05.HentEndringerRespons;
import no.difi.kontaktinfo.xsd.oppslagstjeneste._14_05.Informasjonsbehov;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.HashSet;

import static junit.framework.Assert.assertNotNull;


public class HeleRegisteret {
    
    public static void main(String[] args){
        // Optionally set system property "kontaktinfo.address.location" to override the default test endpoint
        String serviceAddress = System.getProperty("kontaktinfo.address.location");
        if(serviceAddress == null) {
            serviceAddress = "https://kontaktinfo-ws-yt2.difi.no/kontaktinfo-external/ws-v3";
        }

        // Enables running against alternative endpoints to the one specified in the WSDL
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(Oppslagstjeneste1405.class);
        jaxWsProxyFactoryBean.setAddress(serviceAddress);

        // Configures WS-Security
        WSS4JInterceptorHelper.addWSS4JInterceptors(jaxWsProxyFactoryBean);
        Oppslagstjeneste1405 kontaktinfoPort = (Oppslagstjeneste1405) jaxWsProxyFactoryBean.create();

        // Optionally set system property "kontaktinfo.ssl.disable" to disable SSL checks to enable running tests against endpoint with invalid SSL setup

        Client client = ClientProxy.getClient(kontaktinfoPort);
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        TLSClientParameters tlsClientParameters = new TLSClientParameters();
        tlsClientParameters.setDisableCNCheck(true);
        httpConduit.setTlsClientParameters(tlsClientParameters);
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");


        long lokalStatus = 0;
        long siste = -1;


        HashSet<String> ssns = new HashSet<String>();

        System.out.println("Starting test");
        long start = System.currentTimeMillis();
        long personCount = 0;
        do{

            try{
                HentEndringerForespoersel endringerForespoersel = new HentEndringerForespoersel();
                endringerForespoersel.getInformasjonsbehov().add(Informasjonsbehov.KONTAKTINFO);
                endringerForespoersel.setFraEndringsNummer(lokalStatus+1);
                HentEndringerRespons endringerRespons = kontaktinfoPort.hentEndringer(endringerForespoersel);
                for(Person p : endringerRespons.getPerson()){

                    ssns.add(p.getPersonidentifikator());
                    personCount++;
                }
                siste = endringerRespons.getSenesteEndringsNummer();
                lokalStatus = endringerRespons.getTilEndringsNummer();
            }catch(Throwable e){
                System.out.println("Feil i henting, prøver igjen");
                e.printStackTrace();
            }

        }while(lokalStatus < siste);
        long end = System.currentTimeMillis();

        System.out.println("Ended test. Current changenumber: " + lokalStatus + ". Uniqe ssns downloaded: " + ssns.size() + " Persons fetched in total: " + personCount);
        System.out.println("Test done in :" + new Duration(end - start).toString());
    }
}
