package tpagentnegotiation;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Item implements Serializable, Comparable
{

    @Override
    public int compareTo(Object o) {
        try {
            if(!(o instanceof Offer)) {
                throw new Exception();
            }
        }
        catch (Exception e) {
            Logger.getLogger(Offer.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return 0;
    }
    
}
