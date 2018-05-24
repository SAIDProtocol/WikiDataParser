package edu.rutgers.winlab.wikidataparser;

import java.util.Objects;

/**
 *
 * @author Jiachen Chen
 * @param <T1>
 * @param <T2>
 */
public class Tuple2<T1, T2> {

    private T1 v1;
    private T2 v2;

    public Tuple2(T1 v1, T2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public T1 getV1() {
        return v1;
    }

    public void setV1(T1 v1) {
        this.v1 = v1;
    }

    public T2 getV2() {
        return v2;
    }

    public void setV2(T2 v2) {
        this.v2 = v2;
    }

    public void setValues(T1 v1, T2 v2) {
        setV1(v1);
        setV2(v2);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.v1);
        hash = 59 * hash + Objects.hashCode(this.v2);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tuple2<?, ?> other = (Tuple2<?, ?>) obj;
        if (!Objects.equals(this.v1, other.v1)) {
            return false;
        }
        return Objects.equals(this.v2, other.v2);
    }

}