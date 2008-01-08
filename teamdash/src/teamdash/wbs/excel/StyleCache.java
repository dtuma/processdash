package teamdash.wbs.excel;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class StyleCache {

    HSSFWorkbook xls;

    private Map<StyleKey, HSSFCellStyle> styleCache;

    public StyleCache(HSSFWorkbook xls) {
        this.xls = xls;
        this.styleCache = new HashMap<StyleKey, HSSFCellStyle>();
    }


    public void applyStyle(HSSFCell cell, StyleKey styleKey) {
        HSSFCellStyle style = getStyle(styleKey);
        if (style != null)
            cell.setCellStyle(style);
    }

    public HSSFCellStyle getStyle(StyleKey key) {
        if (DEFAULT_STYLE.equals(key))
            return null;

        HSSFCellStyle style = styleCache.get(key);
        if (style == null) {
            style = xls.createCellStyle();
            key.configure(style);
            HSSFFont font = xls.createFont();
            key.configure(font);
            style.setFont(font);
            styleCache.put(key, style);
        }

        return style;
    }

    private static final StyleKey DEFAULT_STYLE = new StyleKey();

}
