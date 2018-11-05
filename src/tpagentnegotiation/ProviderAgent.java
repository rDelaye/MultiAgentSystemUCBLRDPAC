package tpagentnegotiation;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

public class ProviderAgent extends Agent
{
    List<Wish> wishes;
    Map<String, List<Wish>> byDeparture;
    Map<String, List<Wish>> byDestination;
    
    Map<AID, Map<Ticket, Integer[]>> negociations;
    
    private ProviderGUI gui;
    
    @Override
    protected void setup()
    {
        gui = new ProviderGUI(this);
        gui.showGui();
        
        byDeparture = new HashMap<>();
        byDestination = new HashMap<>();
        wishes = new ArrayList<>();
        negociations = new HashMap<>();
        
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
        addBehaviour(new HandleNegociation());
        addBehaviour(new EndNegociation());
    }
    
    @Override
    protected void takeDown()
    {
        System.out.println("Provider agent " + getAID().getName() + " terminating.");
        gui.dispose();
    }

    // send a proposal containing all the asked items available, while registering the negociators
    private class OfferProposeServer extends CyclicBehaviour
    {
        @Override
        public void action()
        {            
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage message = myAgent.receive(mt);
            if(message != null)
            {
                Ticket desired;
                try
                {
                    desired = (Ticket)message.getContentObject();
                    AID sender = message.getSender();
                    ACLMessage reply = message.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    
                    ArrayList<Offer> offers = new ArrayList<>();
                    for(Wish w : byDeparture.get(desired.getDeparture())) {
                        if(w.t.equals(desired)) {
                            // computing the first price
                            int inflation = (int) (Math.random() * (5)) + 5;
                            int price = w.desiredPrice + w.desiredPrice*inflation/100;
                            offers.add(new Offer(desired, price));
                            // registering the negociation
                            if(negociations.get(sender) == null) {
                                negociations.put(sender, new HashMap<>());
                            }
                            if(negociations.get(sender).get(desired) == null) {
                                negociations.get(sender).put(desired, new Integer[4]);
                            }
                            negociations.get(sender).get(desired)[0] = price;
                        }
                    }
                    if (!offers.isEmpty()) { // The agent has something to sell
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContentObject(offers);
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
    
    // perform the negociations
    private class HandleNegociation extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("ticket-trading"),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));
            ACLMessage proposal = receive(mt);
            if(proposal != null) {
                ACLMessage reply = proposal.createReply();
                try {
                    AID negociator = proposal.getSender();
                    Offer o = (Offer)proposal.getContentObject();
                    Ticket t = (Ticket)o.getSubject();
                    int wantedPrice = o.getPrice();
                    int newPrice = 0;

                    // get the wish associated to the offer
                    Wish wish = null;
                    for(Wish w : byDeparture.get(t.getDeparture())) {
                        if(w.t.compareTo(t) == 0) {
                            wish = w;
                        }
                    }
                    if(wish == null){
                        block();
                    }
                    // check if price is desired
                    if(wantedPrice >= wish.desiredPrice) {
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContentObject(t);
                        myAgent.send(reply);
                    } else {
                        Integer[] steps = negociations.get(negociator).get(t);
                        for(int i = 1; i<steps.length; i++) {
                            if(steps[i] == 0 && steps[i-1] != 0) {
                                newPrice = steps[i-1] + (steps[i-1] - wish.limit)/2;
                                // the negociator already proposes a more attractive price
                                if(newPrice < wantedPrice) {
                                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    reply.setContentObject(t);
                                } else {
                                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    reply.setContentObject(new Offer(t, newPrice));
                                }
                            }
                            // no more proposals (this sould not happen)
                            if(i == 3 && steps[i] != 0) {
                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                reply.setContentObject(t);
                            }
                        }
                    }
                } catch (UnreadableException ex) {
                    Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }   
            } else {
                block();
            }
        }
    }
    
    private class EndNegociation extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REFUSE),
                    MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)));
            ACLMessage msg = receive(mt);
            if(msg != null) {
                if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    // check if item still available
                    
                } else {
                    // clean wishes and negociations
                    
                }
            } else {
                block();
            }
        }
        
    }
    
    public void addWish(Wish w) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                wishes.add(w);
                if(byDeparture.get(w.t.getDeparture()) == null) {
                    byDeparture.put(w.t.getDeparture(), new ArrayList<>());
                }
                byDeparture.get(w.t.getDeparture()).add(w);
                if(byDestination.get(w.t.getDestination()) == null) {
                    byDestination.put(w.t.getDestination(), new ArrayList<>());
                }
                byDestination.get(w.t.getDestination()).add(w);
            }
        });
    }
    private Wish removeWish(Wish w) {
        if(!wishes.remove(w)) {
            return null;
        }
        byDeparture.remove(w.t.getDeparture());
        byDestination.remove(w.t.getDestination());
        return w;
    }
}
