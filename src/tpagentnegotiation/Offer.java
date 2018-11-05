package tpagentnegotiation;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Offer implements Serializable, Comparable
{
    private static int idcnt = 0;
    
    private int id;
    private Item subject;
    private int price;

    public Offer(Item subject, int price)
    {
        this.id = idcnt++;
        this.subject = subject;
        this.price = price;
    }

    public Item getSubject()
    {
        return subject;
    }

    public int getPrice()
    {
        return price;
    }

    @Override
    public int compareTo(Object o) {
        try {
            if(!(o instanceof Offer)) {
                throw new Exception();
            }
            Offer of = (Offer)o;
            return this.price - of.price;
        }
        catch (Exception e) {
            Logger.getLogger(Offer.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return 0;
    }
    
    
}
