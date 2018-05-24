package com.github.igor_anferov.PDFparser;

import java.util.List;

public class Row extends Block {
    Row(List<Block> cells) {
        super(cells.get(0).renderer);
        sons = cells;
    }
}
