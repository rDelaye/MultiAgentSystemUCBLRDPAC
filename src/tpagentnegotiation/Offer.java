package tpagentnegotiation;

import java.io.Serializable;

public class Offer implements Serializable
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
    
    
}
