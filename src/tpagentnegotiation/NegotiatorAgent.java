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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

public class NegotiatorAgent extends Agent {
    List<Wish> wishes;
    
    /* contains all the tickets sold bysome providers, with the list of providers
    selling them associated with an array of the prices asked by the negociator */
    Map<Ticket, List<Pair<AID, Integer[]>>> providers;
    
    // contains the best offer so far for each buyable ticket, with the provider and the price offered
    Map<Ticket, Pair<AID, Integer>> bestOffers;
    
    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args == null || args.length != 3) {
            takeDown();
        }
        String date = (String) args[2];
        String[] parts = date.split("/");
        Ticket ticket = new Ticket((String)args[0], (String)args[1], new GregorianCalendar(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])).getTime());
        Wish w = new Wish(ticket, 700, 900);
        wishes = new ArrayList<>();
        wishes.add(w);
        
        providers = new HashMap<>();
        bestOffers = new HashMap<>();
        
        callForProviders();
        if(providers.isEmpty()) {
            takeDown();
        }
        addBehaviour(new StartNegociation());
        addBehaviour(new HandleNegociation());
        addBehaviour(new EndNegociation());
    }
    
    @Override
    protected void takeDown() {
        System.out.println("Negotiator agent " + getAID().getName() + " terminating.");
    }

    private void callForProviders() {            
        AID[] providerAgents;
        long unique = System.currentTimeMillis();
        
        try 
        {
            // get all providers
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("provider");
            template.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, template);
            providerAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i)
            {
                providerAgents[i] = result[i].getName();
            }
            
            // Creating the list of wanted tickets
            ArrayList<Ticket> tickets = new ArrayList<>();
            for(int i = 0; i<wishes.size(); i++) {
                tickets.add((Ticket)wishes.get(i).t);
            }
            
            // asking for the wanted tickets
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (int j = 0; j < providerAgents.length; ++j) {
                cfp.addReceiver(providerAgents[j]);
            }
            cfp.setContentObject(tickets);
            cfp.setConversationId("ticket-trading");
            cfp.setReplyWith("" + unique);
            this.send(cfp);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        catch (IOException ex)
        {
            Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            wait(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        // geting the positive answeres to register the providers
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                MessageTemplate.and(MessageTemplate.MatchConversationId("ticket-trading"),
                MessageTemplate.MatchInReplyTo(String.valueOf(unique))));
        ACLMessage reply;
        ArrayList<Offer> inStock;
        do {
            reply = this.receive(mt);
            if(reply != null) {
                try {
                    inStock = (ArrayList)reply.getContentObject();
                    for(Offer o : inStock) {
                        Ticket t = (Ticket)o.getSubject();
                        if(!providers.containsKey(t)) {
                            providers.put(t, new ArrayList<>());
                        }
                        providers.get(t).add(new Pair<>(reply.getSender(), new Integer[3]));
                        if(!bestOffers.containsKey(t)) {
                            bestOffers.put(t, new Pair<>(reply.getSender(), o.getPrice()));
                        } else {
                            if(bestOffers.get(t).getValue() > o.getPrice()) {
                                bestOffers.replace(t, new Pair<>(reply.getSender(), o.getPrice()));
                            }
                        }
                    }
                } catch (UnreadableException ex) {
                    Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        while(reply != null);
    }
    
    private class StartNegociation extends OneShotBehaviour {
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            Iterator it = providers.entrySet().iterator();
            Ticket t;
            List<Pair<AID, Integer[]>> l;
            // for each item to buy
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                t = (Ticket)entry.getKey();
                l = (ArrayList)entry.getValue();
                // for each seller
                for(Pair<AID, Integer[]> p : l) {
                    for(Wish w : wishes) {
                        if(w.t == t) {
                            msg.addReceiver(p.getKey());
                            int inflation = (int) (Math.random() * (5)) + 5;
                            int price = w.desiredPrice - w.desiredPrice*inflation/100;
                            // if the price to ask is smaller than the previous offer
                            if(price < bestOffers.get(t).getValue()) {
                                try {
                                    msg.setContentObject(new Offer(t, price));
                                    // register the first price asked
                                    p.getValue()[0] = price;
                                } catch (IOException ex) {
                                    Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                msg.addReceiver(p.getKey());
                                try {
                                    msg.setContentObject(t);
                                } catch (IOException ex) {
                                    Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            msg.setReplyWith("" + System.currentTimeMillis());
                            myAgent.send(msg);
                        }
                    }
                }
            }
        }
    }
    
    private class HandleNegociation extends Behaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("ticket-trading"),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));
            ACLMessage proposal = receive(mt);
            if(proposal != null) {
                ACLMessage reply = proposal.createReply();
                try {
                    AID provider = proposal.getSender();
                    Offer o = (Offer)proposal.getContentObject();
                    Ticket t = (Ticket)o.getSubject();
                    int wantedPrice = o.getPrice();

                    // get the wish associated to the offer
                    Wish wish = null;
                    for(Wish w : wishes) {
                        if(w.t == t) {
                            wish = w;
                        }
                    }
                    if(wish == null){
                        block();
                    }
                    // check if price is desired
                    if(wantedPrice <= wish.desiredPrice) {
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContentObject(t);
                        myAgent.send(reply);
                    } else {
                        for(Pair<AID, Integer[]> p : providers.get(t)) {
                            if(p.getKey() == provider) {
                                // check if this a third offer and is better than the current best
                                if(bestOffers.get(t) == null) {
                                    bestOffers.put(t, new Pair(provider, wantedPrice));
                                }
                                if(p.getValue()[2] != 0 && p.getValue()[2] < bestOffers.get(t).getValue()) {
                                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    reply.setContentObject(t);
                                    myAgent.send(reply);
                                } else { // this is not the third offer
                                    // update the best price
                                    if(wantedPrice < bestOffers.get(t).getValue()) {
                                        bestOffers.replace(t, new Pair<>(provider, wantedPrice));
                                    }
                                    // get last proposed price to this provider
                                    int i = 0;
                                    if(p.getValue()[1] != 0) {
                                        i = 1;
                                    }
                                    int lastPrice = p.getValue()[i];
                                    int newPrice = lastPrice + (wish.limit-lastPrice)/2;
                                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    reply.setContentObject(new Offer(t, newPrice));
                                    p.getValue()[i++] = newPrice;
                                    // propose this new price
                                    myAgent.send(reply);
                                }
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

        @Override
        public boolean done() {
            return providers.isEmpty();
        }
    }
    
    private class EndNegociation extends Behaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("ticket-trading"),
                    MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REFUSE),
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL))));
            ACLMessage msg = receive(mt);
            if(msg != null) {
                AID sender = msg.getSender();
                if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    try {
                        Ticket target = (Ticket)msg.getContentObject();
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContentObject(target);

                        // if the ticket is still wanted, remove it from the wishlist and conclude the deal
                        Iterator it = wishes.iterator();
                        Wish tmp;
                        while(it.hasNext()) {
                            tmp = (Wish) it.next();
                            if(tmp.t.compareTo(target) == 0) {
                                it.remove();
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContentObject(target);
                                break;
                            }
                        }
                        myAgent.send(reply);
                    } catch (UnreadableException ex) {
                        Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    try {
                        Ticket t = (Ticket) msg.getContentObject();
                        bestOffers.remove(t);
                        if(msg.getPerformative() == ACLMessage.REFUSE) {
                            for(int i = 0; i < providers.get(t).size(); i++) {
                                if (providers.get(t).get(i).getKey() == sender) {
                                   providers.get(t).remove(i);
                                }
                            }
                            if(providers.get(t).isEmpty()) {
                                providers.remove(t);
                            }
                        } else {
                            // got the desired ticket, can remove from the wishlist
                            providers.remove(t);
                            Iterator it = wishes.iterator();
                            Wish tmp;
                            while(it.hasNext()) {
                                tmp = (Wish) it.next();
                                if(tmp.t.compareTo(t) == 0) {
                                    it.remove();
                                    break;
                                }
                            }
                        }
                    } catch (UnreadableException ex) {
                        Logger.getLogger(NegotiatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return providers.isEmpty();
        }
        
    }
}
