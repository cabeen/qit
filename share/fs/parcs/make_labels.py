#! /usr/bin/env python

def read_rows(fn):
    return [line.strip().split(",")[0:2] for line in open(fn)][1:]

def make_lut(rows):
    out = {}
    for row in rows:
        out[row[0]] = row[1]
    return out

def make_reverse_lut(rows):
    out = {}
    for row in rows:
        out[row[1]] = row[0]
    return out

if __name__ == "__main__":
    for n in ["bm", "gm", "wm"]:
        print "processing %s" % n
        name_map = read_rows("dk%s.lb%s.name.map.csv" % (n, n))

        lb = read_rows("lb%s.csv" % n)
        dk = read_rows("dk%s.csv" % n)
        lb_lut = make_reverse_lut(lb)
        dk_lut = make_reverse_lut(dk)

        idx_map = [] 
        for row in name_map:
            idx_map.append((dk_lut[row[0]], lb_lut[row[1]]))
        idx_lut = make_lut(idx_map)

        f = open("dk%s.lb%s.map.csv" % (n,n), "w")
        f.write("from,to\n")
        for row in idx_map: 
            f.write("%s,%s\n" % row)
        f.close()

        wmparc_dk = read_rows("wmparc.dk%s.map.csv" % n)
        dk_lb_lut = make_lut(idx_map)

        f = open("wmparc.lb%s.map.csv" % n, "w")
        f.write("from,to\n")
        for row in wmparc_dk:
            f.write("%s,%s\n" % (row[0], idx_lut[row[1]]))
        f.close()
