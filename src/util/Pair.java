package util;

import java.io.Serializable;

public class Pair<N, V> implements Serializable {
    public N first;
    public V second;

    public Pair(N name, V value) {
        this.first = name;
        this.second = value;
    }

    public void setFirst(N first) {
        this.first = first;
    }

    public N getFirst() {
        return first;
    }

    public void setSecond(V second) {
        this.second = second;
    }

    public V getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair) {
            Pair pair = (Pair) obj;
            return (this.getFirst().equals(pair.getFirst()) && this.getSecond().equals(pair.getSecond()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getFirst().hashCode() ^ this.getSecond().hashCode();
    }

    public String toString()
    {
        return "PAIR<" + this.getFirst() + " -- " + this.getSecond()+ ">";
    }

}
