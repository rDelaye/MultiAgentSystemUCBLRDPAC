package tpagentnegotiation;

import java.io.IOException;
import java.util.Date;

public class Billet extends Item
{
    private String destination;
    private String depart;
    private Date jourDepart;

    public Billet(String destination, String depart, Date jourDepart)
    {        
        this.destination = destination;
        this.depart = depart;
        this.jourDepart = jourDepart;
    }    

    public String getDestination()
    {
        return destination;
    }

    public String getDepart()
    {
        return depart;
    }

    public Date getJourDepart()
    {
        return jourDepart;
    }
     
    public boolean match(Item i)
    {
        if(!(i instanceof Billet))
        {
            return false;
        }
        Billet b = (Billet)i;
        if(b.depart.equals(this.depart) && b.destination.equals(this.destination) && b.jourDepart.equals(this.jourDepart))
            return true;
        return false;
    }
}
