// Verilog netlist generated by Workcraft 3 -- http://workcraft.org/
module VME (d, lds, dtack, dsr, dsw, ldtack);
    input dsr, dsw, ldtack;
    output d, lds, dtack;

    NAND3B U1 (.ON(U1_ON), .AN(OUT_BUBBLE3_ON), .B(ldtack), .C(dsr));
    // This inverter should have a short delay
    INV IN_BUBBLE3 (.ON(IN_BUBBLE3_ON), .I(OUT_BUBBLE2_ON));
    // This inverter should have a short delay
    INV IN_BUBBLE5 (.ON(IN_BUBBLE5_ON), .I(ldtack));
    OAI221 U7 (.ON(U7_ON), .A1(IN_BUBBLE3_ON), .A2(d), .B1(IN_BUBBLE5_ON), .B2(OUT_BUBBLE3_ON), .C(dsw));
    NAND2 U8 (.ON(d), .A(U7_ON), .B(U1_ON));
    // This inverter should have a short delay
    INV IN_BUBBLE10 (.ON(IN_BUBBLE10_ON), .I(OUT_BUBBLE3_ON));
    INV OUT_BUBBLE1 (.ON(OUT_BUBBLE1_ON), .I(U14_ON));
    OAI221 U14 (.ON(U14_ON), .A1(d), .A2(dsr), .B1(dsr), .B2(OUT_BUBBLE2_ON), .C(IN_BUBBLE10_ON));
    // This inverter should have a short delay
    INV IN_BUBBLE16 (.ON(IN_BUBBLE16_ON), .I(OUT_BUBBLE2_ON));
    // This inverter should have a short delay
    INV IN_BUBBLE18 (.ON(IN_BUBBLE18_ON), .I(dsw));
    OAI31 U20 (.ON(U20_ON), .A1(IN_BUBBLE18_ON), .A2(IN_BUBBLE16_ON), .A3(d), .B(OUT_BUBBLE3_ON));
    C2 U21 (.Q(lds), .A(U20_ON), .B(OUT_BUBBLE1_ON));
    // This inverter should have a short delay
    INV IN_BUBBLE23 (.ON(IN_BUBBLE23_ON), .I(OUT_BUBBLE3_ON));
    // This inverter should have a short delay
    INV IN_BUBBLE25 (.ON(IN_BUBBLE25_ON), .I(OUT_BUBBLE2_ON));
    AOI221 U26 (.ON(dtack), .A1(IN_BUBBLE23_ON), .A2(dsw), .B1(d), .B2(OUT_BUBBLE3_ON), .C(IN_BUBBLE25_ON));
    // This inverter should have a short delay
    INV IN_BUBBLE28 (.ON(IN_BUBBLE28_ON), .I(OUT_BUBBLE3_ON));
    INV OUT_BUBBLE2 (.ON(OUT_BUBBLE2_ON), .I(U31_ON));
    OAI222 U31 (.ON(U31_ON), .A1(IN_BUBBLE28_ON), .A2(dsw), .B1(OUT_BUBBLE2_ON), .B2(d), .C1(d), .C2(lds));
    // This inverter should have a short delay
    INV IN_BUBBLE33 (.ON(IN_BUBBLE33_ON), .I(d));
    INV OUT_BUBBLE3 (.ON(OUT_BUBBLE3_ON), .I(U36_ON));
    AOI32 U36 (.ON(U36_ON), .A1(IN_BUBBLE33_ON), .A2(ldtack), .A3(OUT_BUBBLE2_ON), .B1(ldtack), .B2(OUT_BUBBLE3_ON));

    // signal values at the initial state:
    // IN_BUBBLE5_ON U14_ON IN_BUBBLE33_ON IN_BUBBLE10_ON IN_BUBBLE16_ON IN_BUBBLE18_ON !OUT_BUBBLE1_ON U20_ON !dsr U7_ON IN_BUBBLE25_ON IN_BUBBLE28_ON !d !lds !dtack U36_ON !ldtack U31_ON !OUT_BUBBLE3_ON U1_ON IN_BUBBLE3_ON !dsw IN_BUBBLE23_ON !OUT_BUBBLE2_ON
endmodule
