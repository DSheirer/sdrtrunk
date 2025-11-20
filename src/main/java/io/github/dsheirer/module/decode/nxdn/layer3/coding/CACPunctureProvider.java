package io.github.dsheirer.module.decode.nxdn.layer3.coding;

/**
 * Puncture provider for CAC messages
 */
public class CACPunctureProvider extends PunctureProvider
{
    private static final int BLOCK_SIZE = 14;
    private static final int PUNCTURE_BIT_1 = 3;
    private static final int PUNCTURE_BIT_2 = 11;

    /**
     * Constructs an instance
     */
    public CACPunctureProvider()
    {
        super(BLOCK_SIZE, 2);
    }

    @Override
    public boolean isPreserved(int index)
    {
        int mod = index % BLOCK_SIZE;
        return (mod != PUNCTURE_BIT_1) && (mod != PUNCTURE_BIT_2);
    }

    @Override
    public boolean isPunctured(int index)
    {
        int mod = index % BLOCK_SIZE;
        return (mod == PUNCTURE_BIT_1) || (mod == PUNCTURE_BIT_2);
    }
}
