package completablefuture;

import lombok.Setter;
import lombok.ToString;

@ToString
public class Car {

    int id;
    int manufacturerId;
    String model;
    int year;

    @Setter
    float rating;

    public Car(int id, int manufacturerId, String model, int year) {
        this.id = id;
        this.manufacturerId = manufacturerId;
        this.model = model;
        this.year = year;
    }

}
