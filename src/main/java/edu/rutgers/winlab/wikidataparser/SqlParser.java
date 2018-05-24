package edu.rutgers.winlab.wikidataparser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 *
 * @author jiachen
 */
public class SqlParser {

    public static enum DataType {
        INT, DOUBLE, STRING
    }

    public static ArrayList<QuadConsumer<char[], int[], Object[], int[]>> compileEvents(DataType[] types) {
        ArrayList<QuadConsumer<char[], int[], Object[], int[]>> ret = new ArrayList<>();
        ret.add(SqlParser::skipEmptyString);
        ret.add(SqlParser::assertLeftBracket);
        for (int i = 0; i < types.length - 1; i++) {
            ret.add(SqlParser::skipEmptyString);
            switch (types[i]) {
                case INT:
                    ret.add(SqlParser::readInt);
                    break;
                case DOUBLE:
                    ret.add(SqlParser::readDouble);
                    break;
                case STRING:
                    ret.add(SqlParser::readString);
                    break;
            }
            ret.add(SqlParser::skipEmptyString);
            ret.add(SqlParser::assertComma);
        }
        ret.add(SqlParser::skipEmptyString);
        switch (types[types.length - 1]) {
            case INT:
                ret.add(SqlParser::readInt);
                break;
            case DOUBLE:
                ret.add(SqlParser::readDouble);
                break;
            case STRING:
                ret.add(SqlParser::readString);
                break;
        }
        ret.add(SqlParser::skipEmptyString);
        ret.add(SqlParser::assertRightBracket);
        ret.add(SqlParser::skipEmptyString);
        return ret;
    }

    public static void parseFile(String fileName, DataType[] types, Consumer<Object[]> objectHandler, ReportObject ro) throws IOException {
        ArrayList<QuadConsumer<char[], int[], Object[], int[]>> consumers = compileEvents(types);
        ro.setKey("Line", 100);
        int[] start = new int[1];
        int[] resultIdx = new int[1];
        Object[] result = new Object[types.length];
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            ro.setValue(100, 0);
            while ((line = br.readLine()) != null) {
                int lineId = ro.incrementValue(100);
                if (!line.startsWith("INSERT INTO")) {
                    System.out.printf("%d: %s%n", lineId, line);
                    continue;
                }
                start[0] = line.indexOf("VALUES");
                if (start[0] == -1) {
                    System.out.printf("%d: %s%n", lineId, line);
                    continue;
                }
                char[] l = line.toCharArray();
                start[0] += 7;

                while (true) {
                    resultIdx[0] = 0;
                    consumers.forEach((consumer) -> {
                        consumer.accept(l, start, result, resultIdx);
                    });
                    assert resultIdx[0] == types.length;
                    objectHandler.accept(result);
                    skipEmptyString(l, start, result, resultIdx);
                    if (start[0] == l.length - 1) {
                        assertSemicolon(l, start, result, resultIdx);
                        break;
                    }
                    assertComma(l, start, result, resultIdx);
                    skipEmptyString(l, start, result, resultIdx);
                }
//                return;
            }
        }
    }

    public static void assertLeftBracket(char[] line, int[] start, Object[] result, int[] resultIdx) {
        assert line[start[0]] == '(';
        start[0]++;
    }

    public static void assertComma(char[] line, int[] start, Object[] result, int[] resultIdx) {
//        if (line[start[0]] != ',') {
//            System.out.println("HERE!");
//        }
        assert line[start[0]] == ',';
        start[0]++;
    }

    public static void assertSemicolon(char[] line, int[] start, Object[] result, int[] resultIdx) {
//        if (line[start[0]] != ',') {
//            System.out.println("HERE!");
//        }
        assert line[start[0]] == ';';
        start[0]++;
    }

    public static void assertRightBracket(char[] line, int[] start, Object[] result, int[] resultIdx) {
        assert line[start[0]] == ')';
        start[0]++;
    }

    public static void skipEmptyString(char[] line, int[] start, Object[] result, int[] resultIdx) {
        int pos = start[0];
        keepSkip:
        {
            for (; pos < line.length; pos++) {
                switch (line[pos]) {
                    case ' ':
                    case '\t':
                    case '\r':
                        break;
                    default:
                        break keepSkip;
                }
            }
        }
        start[0] = pos;
    }

    public static void readDouble(char[] line, int[] start, Object[] result, int[] resultIdx) {
        int s = start[0], e = s;
        if (line[s] == '-') {
            e++;
        }
        assert (line[e] >= '0' && line[e] <= '9') || line[e] == '.';
        e++;
        for (; e < line.length; e++) {
            if ((line[e] < '0' || line[e] > '9') && line[e] != '.') {
                break;
            }
        }
        start[0] = e;
        result[resultIdx[0]++] = Double.parseDouble(new String(line, s, e - s));
    }

    public static void readInt(char[] line, int[] start, Object[] result, int[] resultIdx) {
        int s = start[0], e = s;
        if (line[s] == '-') {
            e++;
        }
        assert line[e] >= '0' && line[e] <= '9';
        e++;
        for (; e < line.length; e++) {
            if (line[e] < '0' || line[e] > '9') {
                break;
            }
        }
        start[0] = e;
        result[resultIdx[0]++] = Integer.parseInt(new String(line, s, e - s));
    }

    public static void readString(char[] line, int[] start, Object[] result, int[] resultIdx) {
        int s = start[0], e;
        if (line[s] == 'N') {
            if (line[s + 1] == 'U' && line[s + 2] == 'L' && line[s + 3] == 'L') {
                start[0] += 4;
                result[resultIdx[0]++] = null;
                return;
            }
        }

//        if (line[s] != '\'') {
//            System.out.println("HERE!");
//        }
        assert line[s] == '\'';
        getString:
        {
            for (e = s + 1;; e++) {
                switch (line[e]) {
                    case '\'':
                        break getString;
                    case '\\':
                        e++;
                        break;
                }
            }
        }
        start[0] = e + 1;
        result[resultIdx[0]++] = new String(line, s + 1, e - s - 1);
    }

}
