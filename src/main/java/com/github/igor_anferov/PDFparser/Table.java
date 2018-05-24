package com.github.igor_anferov.PDFparser;

import java.util.List;
import java.util.stream.Collectors;

public class Table extends Block {
    Table(List<List<Block>> table) {
        super(table.get(0).get(0).renderer);
        sons = table.stream().map(Row::new).collect(Collectors.toList());
        type.type = Type.Table;
    }
}
