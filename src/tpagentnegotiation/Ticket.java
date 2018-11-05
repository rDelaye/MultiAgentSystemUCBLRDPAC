package tpagentnegotiation;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Ticket extends Item
{
    private String destination;
    private String departure;
    private Date departureDay;

    public Ticket(String destination, String departure, Date departureDay)
    {        
        this.destination = destination;
        this.departure = departure;
        this.departureDay = departureDay;
    }    

    public String getDestination()
    {
        return destination;
    }

    public String getDeparture()
    {
        return departure;
    }

    public Date getJourDepart()
    {
        return departureDay;
    }
     
    @Override
    public int compareTo(Object o)
    {
        try {
            if(!(o instanceof Ticket))
            {
                throw new Exception();
            }
            Ticket b = (Ticket)o;

            if(b.departure.equals(this.departure) && b.destination.equals(this.destination)) {
                if(b.departureDay.equals(this.departureDay)) {
                    return 0;
                }
                else {
                    return b.departureDay.compareTo(this.departureDay);
                }
            }
        }
        catch(Exception e) {
            Logger.getLogger(Offer.class.getName()).log(Level.SEVERE, null, e);
        }
        return -1;
    }
}
