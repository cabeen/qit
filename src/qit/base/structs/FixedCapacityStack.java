package qit.base.structs;

import java.util.Stack;

public class FixedCapacityStack<T> extends Stack<T>
{
    private int maxSize;

    public FixedCapacityStack(int size)
    {
        super();
        this.maxSize = size;
    }

    @Override
    public T push(T object)
    {
        //If the stack is too big, remove elements until it's the right size.
        while (this.size() >= this.maxSize)
        {
            this.remove(0);
        }
        return super.push(object);
    }
}

