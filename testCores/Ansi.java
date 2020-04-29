class Ansi {
    public static void main(String[] args) {
        String CSI = "\u001B[";
        System.out.print (CSI + "31" + "m");
        System.out.print ("Texto vermelho   31");
        System.out.println (CSI + "m");
        
        System.out.print (CSI + "32" + "m");
        System.out.print ("Texto verde      32");
        System.out.println (CSI + "m");
        
        System.out.print (CSI + "33" + "m");
        System.out.print ("Texto amarelo    33");
        System.out.println (CSI + "m");

        System.out.print (CSI + "34" + "m");
        System.out.print ("Texto azul       34");
        System.out.println (CSI + "m");

        System.out.print (CSI + "35" + "m");
        System.out.print ("violeta          35");
        System.out.println (CSI + "m");

        System.out.print (CSI + "36" + "m");
        System.out.print ("azul marinho     36");
        System.out.println (CSI + "m");

        System.out.print (CSI + "37" + "m");
        System.out.print ("Texto normal    37");
        System.out.println (CSI + "m");

        System.out.print (CSI + "38" + "m");
        System.out.print ("Texto normal     38");
        System.out.println (CSI + "m");
        
        System.out.print (CSI + "39" + "m");
        System.out.print ("Texto normal     39");
        System.out.println (CSI + "m");


        System.out.print (CSI + "41" + "m");
        System.out.print ("Texto vermelho   41");
        System.out.println (CSI + "m");
        
        System.out.print (CSI + "42" + "m");
        System.out.print ("Texto verde      42");
        System.out.println (CSI + "m");
        
        System.out.print (CSI + "43" + "m");
        System.out.print ("Texto amarelo    43");
        System.out.println (CSI + "m");

        System.out.print (CSI + "44" + "m");
        System.out.print ("Texto azul       44");
        System.out.println (CSI + "m");

        System.out.print (CSI + "45" + "m");
        System.out.print ("violeta          45");
        System.out.println (CSI + "m");

        System.out.print (CSI + "46" + "m");
        System.out.print ("azul marinho     46");
        System.out.println (CSI + "m");

        System.out.print (CSI + "47" + "m");
        System.out.print ("Texto normal     47");
        System.out.println (CSI + "m");

        System.out.print (CSI + "48" + "m");
        System.out.print ("Texto normal");
        System.out.println (CSI + "m");
        
        System.out.print (CSI + "49" + "m");
        System.out.print ("Texto normal");
        System.out.println (CSI + "m");

        System.out.println ("Debug");
        
    }
}