import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class TestLogoFidelity {

    public static void main(String[] args) throws Exception {
        int size = 1024;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Transparent background
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);
        g.setComposite(AlphaComposite.SrcOver);

        // Exact dark emerald green from user's image
        Color green = new Color(0x15, 0x73, 0x47);
        g.setColor(green);

        // Coordinate system: 100 x 100
        double scale = size / 100.0;
        float strokeW = (float)(5.4 * scale);
        g.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // 1. House Outline
        // Peak: (50.0, 7.5)
        // Left roof slope: to (4.5, 46.5)
        // Underside eave inward: to (12.0, 46.5)
        // Left wall down: to (12.0, 81.0)
        // Bottom-left rounded corner: to (23.0, 92.0)
        // Bottom floor: to (77.0, 92.0)
        // Bottom-right rounded corner: to (88.0, 81.0)
        // Right wall up: to (88.0, 46.5)
        // Underside eave outward: to (95.5, 46.5)
        // Right roof slope up: to (50.0, 7.5)
        Path2D.Double house = new Path2D.Double();
        house.moveTo(50.0 * scale, 7.5 * scale);
        house.lineTo(4.5 * scale, 46.5 * scale);
        house.lineTo(12.0 * scale, 46.5 * scale);
        house.lineTo(12.0 * scale, 81.0 * scale);
        house.quadTo(12.0 * scale, 92.0 * scale, 23.0 * scale, 92.0 * scale);
        house.lineTo(77.0 * scale, 92.0 * scale);
        house.quadTo(88.0 * scale, 92.0 * scale, 88.0 * scale, 81.0 * scale);
        house.lineTo(88.0 * scale, 46.5 * scale);
        house.lineTo(95.5 * scale, 46.5 * scale);
        house.closePath();
        g.draw(house);

        // 2. Interlocking Infinity Symbol
        // Center of whole symbol is at (50.0, 63.5)
        // Center of left loop is at (34.0, 63.5), radius = 15.0
        // Center of right loop is at (66.0, 63.5), radius = 15.0
        // Left piece:
        Path2D.Double leftPiece = new Path2D.Double();
        // Starts at upper curve coming into the left loop
        leftPiece.moveTo(47.0 * scale, 53.0 * scale);
        leftPiece.curveTo(41.0 * scale, 48.5 * scale, 34.0 * scale, 48.5 * scale, 28.0 * scale, 50.0 * scale);
        leftPiece.curveTo(20.0 * scale, 52.5 * scale, 19.0 * scale, 63.5 * scale, 19.0 * scale, 63.5 * scale);
        leftPiece.curveTo(19.0 * scale, 73.0 * scale, 26.0 * scale, 78.5 * scale, 34.0 * scale, 78.5 * scale);
        leftPiece.curveTo(40.5 * scale, 78.5 * scale, 46.5 * scale, 75.0 * scale, 50.0 * scale, 70.0 * scale);
        leftPiece.lineTo(58.0 * scale, 62.0 * scale);

        // Right piece (180 degree rotation around (50.0, 63.5))
        Path2D.Double rightPiece = new Path2D.Double();
        rightPiece.moveTo(53.0 * scale, 74.0 * scale);
        rightPiece.curveTo(59.0 * scale, 78.5 * scale, 66.0 * scale, 78.5 * scale, 72.0 * scale, 77.0 * scale);
        rightPiece.curveTo(80.0 * scale, 74.5 * scale, 81.0 * scale, 63.5 * scale, 81.0 * scale, 63.5 * scale);
        rightPiece.curveTo(81.0 * scale, 54.0 * scale, 74.0 * scale, 48.5 * scale, 66.0 * scale, 48.5 * scale);
        rightPiece.curveTo(59.5 * scale, 48.5 * scale, 53.5 * scale, 52.0 * scale, 50.0 * scale, 57.0 * scale);
        rightPiece.lineTo(42.0 * scale, 65.0 * scale);

        g.draw(leftPiece);
        g.draw(rightPiece);

        g.dispose();

        ImageIO.write(img, "PNG", new File("/tmp/test_logo_green.png"));
        System.out.println("Rendered green transparent logo");
    }
}
