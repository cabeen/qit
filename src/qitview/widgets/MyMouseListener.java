package qitview.widgets;

import java.awt.event.MouseWheelEvent;

public interface MyMouseListener
{
    public void mouseClicked(MyMouseEvent e);

    public void mousePressed(MyMouseEvent e);

    public void mouseReleased(MyMouseEvent e);

    public void mouseEntered(MyMouseEvent e);

    public void mouseExited(MyMouseEvent e);

    public void mouseDragged(MyMouseEvent e);

    public void mouseMoved(MyMouseEvent e);

    public void mouseWheelMoved(MouseWheelEvent e);
}
