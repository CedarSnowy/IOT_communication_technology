package routing;

import core.Connection;
import core.Message;
import core.Settings;
import util.Tuple;

import java.util.ArrayList;
import java.util.List;

public class MyRouter extends ActiveRouter {
    public MyRouter(Settings var1) {
        super(var1);
    }

    protected MyRouter(MyRouter var1) {
        super(var1);
    }

    public void update() {
        super.update();
        }
    public MyRouter replicate() {
        return new MyRouter(this);
    }

}
