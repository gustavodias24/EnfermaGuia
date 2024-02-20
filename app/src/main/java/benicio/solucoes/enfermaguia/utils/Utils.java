package benicio.solucoes.enfermaguia.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class Utils {
    private static AtomicInteger sNextGeneratedId = new AtomicInteger(1);


    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // A range of IDs is reserved for usages with View.setId.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over at 0x00FFFFFF to avoid collisions with generated ids.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }
}
