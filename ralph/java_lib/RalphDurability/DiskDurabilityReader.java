package RalphDurability;

import java.io.FileInputStream;
import java.io.IOException;

import ralph.Util;

import ralph_protobuffs.DurabilityProto.Durability;

public class DiskDurabilityReader implements IDurabilityReader
{
    private final FileInputStream f_input;
    
    public DiskDurabilityReader(String filename)
    {
        f_input = f_input_creator(filename);
    }

    private FileInputStream f_input_creator(String filename)
    {
        try
        {
            return new FileInputStream(filename);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Could not initialize file reader");
            return null;
        }
    }

    @Override
    public synchronized Durability get_durability_msg()
    {
        try
        {
            return Durability.parseDelimitedFrom(f_input);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Error reading from file");
            return null;
        }
    }
}