package tpagentnegotiation;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProviderAgent extends Agent
{
    Billet billet;
    int desiredPrice;
    int minPrice;
    
    @Override
    protected void setup()
    {
        billet = new Billet("Narita", "Lyon", new GregorianCalendar(2019, Calendar.FEBRUARY, 11).getTime());
        desiredPrice = 1000;
        minPrice = 800;
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("provider");
        sd.setName("JADE-item-provider");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        addBehaviour(new OfferProposeServer());
        addBehaviour(new ValidateServer());

    }
    
    @Override
    protected void takeDown()
    {
        
    }

    private class ValidateServer extends CyclicBehaviour
    {

        @Override
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage message = myAgent.receive(mt);
            if(message != null)
            {
                Offer finalOffer;
                try
                {
                    finalOffer = (Offer)message.getContentObject();
                    ACLMessage reply = message.createReply();
                    if(billet.match(finalOffer.getSubject()))
                    {
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContentObject(finalOffer);
                    }
                    else
                    {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("43: Not available");
                    }
                    myAgent.send(reply);
                } catch (IOException | UnreadableException ex)
                {
                    Logger.getLogger(ProviderAgent.class.getName()).log(Level.SEVERE, null, ex);
                }                                   
            }
            else
            {
                block();
            }
        }
    }

    private class OfferProposeServer extends CyclicBehaviour
    {
        @Override
        public void action()
        {            
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage message = myAgent.receive(mt);
            if(message != null)
            {
                Item desired;
                try
                {
                    desired = (Item)message.getContentObject();
                    ACLMessage reply = message.createReply();
                    if(billet.match(desired))
                    {
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContentObject(new Offer(billet, desiredPrice));
                    }
                    else
                    {
                        reply.setPerformative(ACLMessage.REFUSE);
                    }
                    myAgent.send(reply);
                } catch (IOException | UnreadableException ex)
                {
                    Logger.getLogger(ProviderAgent.class.getName()).log(Level.SEVERE, null, ex);
                }                                   
            }
            else
            {
                block();
            }
        }
    }
    
    
}
