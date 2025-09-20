package com.tenten.dto;

import com.tenten.CPOException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Investment {

    public enum Frequency {
        YEARLY(1),
        QUARTERLY(4),
        MONTHLY(12),
        DAILY(365);

        private final int value;

        Frequency(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private Float initial;
    private Float interestRate;
    private Integer duration;  // Years
    private Float contribution;
    private Frequency frequency;


    public static Investment decode(DTOMap row) {
        try {
            return new Investment(
                    Float.valueOf(row.get("initial")),
                    Float.valueOf(row.get("interestRate")),
                    Integer.valueOf(row.get("duration")),
                    Float.valueOf(row.get("contribution")),
                    Frequency.valueOf(row.get("frequency")));
        }
        catch (NumberFormatException nfe) {
            throw new CPOException("Invalid Feature Parameters");
        }
    }
}
