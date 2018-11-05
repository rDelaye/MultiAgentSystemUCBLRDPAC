package tpagentnegotiation;

public class Wish {
    public Ticket t;
    public int limit;
    public int desiredPrice;

    public Wish(Ticket t, int d, int l) {
        this.t = t;
        limit = l;
        desiredPrice = d;
    }

    public Wish() {
        this.t = new Ticket(null, null, null);
        limit = 0;
        desiredPrice = 0;
    }
}
