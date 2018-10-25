package tpagentnegotiation;

import jade.core.AID;
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

public class NegotiatorAgent extends Agent
{
    Billet billet;
    int desiredPrice;
    int maxPrice;
    
    
    @Override
    protected void setup()
    {
        billet = new Billet("Narita", "Lyon", new GregorianCalendar(2019, Calendar.FEBRUARY, 11).getTime());
        desiredPrice = 700;
        maxPrice = 850;
        
        addBehaviour(new AskForItem());
        addBehaviour(new NegociateItem());
    }
    
    @Override
    protected void takeDown()
    {
        
    }

    private class NegociateItem extends CyclicBehaviour
    {

        @Override
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage message = myAgent.receive(mt);
            if(message != null)
            {
                Offer o;
                try
                {
                    o = (Offer) message.getContentObject();
                    if(o.getPrice() < maxPrice)
                    {
                        ACLMessage reply = message.createReply();
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContentObject(o);
                    }
                    else
                    {
                        // :'(
                    }
                } catch (UnreadableException | IOException ex)
                {
                    Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                
                    
            }
            else
            {
                block();
            }
            
        }
    }

    private class AskForItem extends Behaviour
    {            
        private AID[] providerAgents;
        @Override
        public void action()
        {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("provider");
            template.addServices(sd);
            try 
            {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                providerAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) 
                {
                    providerAgents[i] = result[i].getName();
                }                                                           
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (int j = 0; j < providerAgents.length; ++j) {
                cfp.addReceiver(providerAgents[j]);
            } 
            try
            {
                cfp.setContentObject(billet);
            } catch (IOException ex)
            {
                Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            cfp.setConversationId("billet-trade");
            cfp.setReplyWith("" + System.currentTimeMillis()); // Unique value
            myAgent.send(cfp);            
        }

        @Override
        public boolean done()
        {
            return true;
        }
    }
    
}
