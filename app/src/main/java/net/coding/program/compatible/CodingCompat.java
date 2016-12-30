package net.coding.program.compatible;

/**
 * Created by chenchao on 2016/12/28.
 */

public class CodingCompat implements ClassCompatInterface {

    private static ClassCompatInterface substance;

    private CodingCompat() {}

    public static void init(ClassCompatInterface substance) {
        CodingCompat.substance = substance;
    }

    public static ClassCompatInterface instance() {
        return substance;
    }

    @Override
    public Class<?> getMainActivity() {
        return substance.getMainActivity();
    }
}