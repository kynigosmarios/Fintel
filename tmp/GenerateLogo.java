import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateLogo {
    public static void main(String[] args) throws Exception {
        int size = 1024;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        // Pure White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        
        // Exact Green: #157347
        Color green = new Color(0x15, 0x73, 0x47);
        g.setColor(green);
        
        // Scale to 1024x1024 from 100x100
        // Padding: 5% around edges to match user's image
        double scale = size / 100.0;
        float strokeW = (float)(6.2 * scale);
        g.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // 1. House Path
        // Peak at (50, 6.5)
        // Left roof slope to (5.0, 46.5)
        // Eave underside horizontal inward to (11.5, 46.5)
        // Left wall straight down to (11.5, 81.5)
        // Bottom-left rounded corner to (21.5, 91.5)
        // Bottom floor to (78.5, 91.5)
        // Bottom-right rounded corner to (88.5, 81.5)
        // Right wall straight up to (88.5, 46.5)
        // Eave underside horizontal outward to (95.0, 46.5)
        // Right roof slope up to (50.0, 6.5)
        Path2D.Double house = new Path2D.Double();
        house.moveTo(50.0 * scale, 6.5 * scale);
        house.lineTo(5.0 * scale, 46.5 * scale);
        house.lineTo(11.5 * scale, 46.5 * scale);
        house.lineTo(11.5 * scale, 81.5 * scale);
        house.quadTo(11.5 * scale, 91.5 * scale, 21.5 * scale, 91.5 * scale);
        house.lineTo(78.5 * scale, 91.5 * scale);
        house.quadTo(88.5 * scale, 91.5 * scale, 88.5 * scale, 81.5 * scale);
        house.lineTo(88.5 * scale, 46.5 * scale);
        house.lineTo(95.0 * scale, 46.5 * scale);
        house.closePath();
        
        g.draw(house);
        
        // 2. Interlocking Infinity Symbol
        // Centered horizontally at x=50. Center Y at y=66.0.
        // Loop radius ~14.5. Loop centers: Left at (32.5, 66.0), Right at (67.5, 66.0)
        // Left piece:
        Path2D.Double leftPiece = new Path2D.Double();
        // Starts at upper diagonal
        leftPiece.moveTo(42.5 * scale, 55.5 * scale);
        leftPiece.curveTo(39.0 * scale, 52.0 * scale, 34.5 * scale, 50.0 * scale, 29.5 * scale, 50.0 * scale);
        leftPiece.curveTo(20.0 * scale, 50.0 * scale, 12.5 * scale, 57.0 * scale, 12.5 * scale, 66.0 * scale);
        leftPiece.curveTo(12.5 * scale, 75.0 * scale, 20.0 * scale, 82.0 * scale, 29.5 * scale, 82.0 * scale);
        leftPiece.curveTo(35.5 * scale, 82.0 * scale, 41.0 * scale, 78.5 * scale, 45.0 * scale, 73.0 * scale);
        leftPiece.lineTo(58.0 * scale, 60.5 * scale);
        
        // Right piece (180 deg rotation about (50, 66))
        Path2D.Double rightPiece = new Path2D.Double();
        rightPiece.moveTo(57.5 * scale, 76.5 * scale);
        rightPiece.curveTo(61.0 * scale, 80.0 * scale, 65.5 * scale, 82.0 * scale, 70.5 * scale, 82.0 * scale);
        rightPiece.curveTo(80.0 * scale, 82.0 * scale, 87.5 * scale, 75.0 * scale, 87.5 * scale, 66.0 * scale);
        rightPiece.curveTo(87.5 * scale, 57.0 * scale, 80.0 * scale, 50.0 * scale, 70.5 * scale, 50.0 * scale);
        rightPiece.curveTo(64.5 * scale, 50.0 * scale, 59.0 * scale, 53.5 * scale, 55.0 * scale, 59.0 * scale);
        rightPiece.lineTo(42.0 * scale, 71.5 * scale);
        
        g.draw(leftPiece);
        g.draw(rightPiece);
        
        g.dispose();
        
        ImageIO.write(img, "PNG", new File("/tmp/generated_logo_preview.png"));
        System.out.println("Preview generated successfully: 1024x1024");
    }
}
