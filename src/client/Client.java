// Client.java
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;

class SocketReader implements Runnable {
  private BufferedReader in;

  public SocketReader(BufferedReader in) {
    this.in = in;
  }

  public void run() {
    boolean connected = true;
    while(connected) {
      try {
        // ler resposta do servidor
        String msg = this.in.readLine();

        if(msg == null) {
          connected = false;
        } else {
          System.out.println("\n" + msg);
        }
      } catch(IOException e) {
        connected = false;
      }
      catch(Exception e) {
        connected = false;
      }
    }
  }
}
  
class Client {
  public static void main(String[] args){
    boolean waitRegister = true;

    if(args.length != 2) {
      System.out.println("missing arguments");
      System.exit(1);
    }

    try{
	    String address = args[0];
	    int port = Integer.parseInt(args[1]);

	    Socket socket = new Socket(address, port);
	    System.out.println("connected!");

	    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

	    boolean autoFlush = true;
	    PrintWriter out = new PrintWriter(socket.getOutputStream(), autoFlush);

	    // para leitura do teclado
	    BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

	    // menu do cliente
	    UtilClient.menu();
	    boolean connected = true;
	    String msg = null;
	    int verifyNumberOption = 0;
	    do {
	        try {
	            connected = false;
	            System.out.print("Insira a opção: ");
	            msg = keyboard.readLine();
	            verifyNumberOption = UtilClient.verifyNumberOption(msg);
	        }
	        catch (NumberFormatException ex3) {
	            System.out.println(ex3.getMessage());
	            connected= true;
	        }
	        catch (NumberInvalidException ex4) {
	            System.out.println(ex4.getMessage());
	            connected= true;
	        }
	    } while (connected);

	    out.println(verifyNumberOption);  // envia a opcao para o server

	    if (verifyNumberOption == 0) {  // saiu do programa
	        System.out.println("Obrigado por utilizar o programa !");
		    System.out.println("disconnected!");
		    socket.shutdownInput();
		    socket.shutdownOutput();
		    socket.close();
	        System.exit(0);
	    }
	    
	    if (verifyNumberOption == 1) {
	        String authQ = "Insira autenticacao (username password): ";
	        System.out.print(authQ);
	        msg = keyboard.readLine();
	        if(msg == null || msg.equals("quit")){
	            connected = false;
	        }
	        else{
	          out.println(msg);
	        }

	        msg = in.readLine();
	        if (msg.equals("PC")) {
	            System.out.println("\n\tAutenticação efetuada com sucesso!\n");
	            connected = true;
	        }
	        else if (msg.equals("PI")) {
	            System.out.println("Password incorreta! Tente novamente");
	            connected = false;
	        }
	        else {
	            System.out.println("\n\tUtilizador não registado!\n");
	            connected = false;
	        }
	    }
	    if (verifyNumberOption == 2) {
	        String register = "Insira dados para o registo (username password): ";
	        System.out.print(register);
	        msg = keyboard.readLine();
	        out.println(msg);
	        String resp = in.readLine();
	        if (resp.equals("UE")) {
	            System.out.println("\n\tUtilizador já registado!\n");
	            connected = false;
	        }
	        else if (resp.equals("RS")) {
	            System.out.println("\n\tUtilizador registado com sucesso!\n");
	            connected = true;
	        }
	    }

	    if(connected){
	    	Thread reader = new Thread(new SocketReader(in));
	    	reader.start();
	    }

	    while(connected) {
	    	boolean verify = false;
	      	try {
	        	//System.out.print("casos: ");
	        	// ler algo do keyboard
	        	boolean flag = false;
	        	do{
			        try{
			        	msg = keyboard.readLine();
			        	verify = UtilClient.verifyCases(msg);
			        	flag = true;
			        }
			        catch(NumberFormatException e){
			      		flag = false;
			      		System.out.println(e.getMessage());
			      	}
			      	catch(NumberInvalidException e){
			      		flag = false;
			      		System.out.println(e.getMessage());
			      	}
			    }
			    while(!flag);

		        if(msg == null || msg.equals("quit") || !verify) {
		          connected = false;
		        } 
		        else {
		          // escrever esse mesmo algo no servidor
		          out.println(msg);
		        }
	      	} 
	      	catch(IOException e) {
	        	connected = false;
	      	}
	      
	      // desnecessária por causa do autoFlush = true
	      // out.flush();
	    }
		socket.shutdownInput();
		socket.shutdownOutput();
	    socket.close();
	}
	catch(IOException e){
		System.out.println("Erro na comunicação. Tente novamente.");
	}
	finally{
		System.out.println("disconnected!");
	    System.exit(1);
	}
  }
}

class UtilClient{
  public static int verifyPort(String s) throws NumberFormatException, NumberPortInvalidException{
    try{
      int n = Integer.parseInt(s);
      if(n<3000){
        throw new NumberPortInvalidException("Numero da porta Tem de ser maior que 3000");
      }   
      else
        return n;
    }
    catch(NumberFormatException e){
      throw new NumberFormatException("Porta tem de ser um número inteiro");
    }
    
  }

  public static void menu() {
      System.out.println("**********************************");
      System.out.println("");
      System.out.println("\t 1. Login");
      System.out.println("\t 2. Register");
      System.out.println("\t 0. Exit");
      System.out.println("");
      System.out.println("**********************************");
  }

  public static int verifyNumberOption(String s) throws NumberFormatException, NumberInvalidException {
    int n = 0;
    try{
      n = Integer.parseInt(s);
      if(n == 1 || n == 0 || n == 2){
        return n;
      }
      else{
        throw new NumberInvalidException("Opcao tem de ser 0 ou 1");
      }
    }
    catch(NumberFormatException e){
      throw new NumberFormatException("Numero tem de ser um inteiro");
    }
  }

	public static boolean verifyCases(String msg) throws NumberFormatException, NumberInvalidException{
		int cases = 0;
		if(msg.equals("quit"))
			return true;
		else{
			try{
				cases = Integer.parseInt(msg);
				if(cases < 0 )
					throw new NumberInvalidException("Número de casos não pode ser negativo!");
				return true;
			}
			catch(NumberFormatException e){
				throw new NumberFormatException("Numero tem de ser um inteiro");
			}	
		}
	}

}


class NumberPortInvalidException extends Exception{
	NumberPortInvalidException(String s){
		super(s);
	}
}
class NumberInvalidException extends Exception{
	NumberInvalidException(String s){
		super(s);
	}
}

//javac DataBaseServejava; javac ClientData.java; javac Server.java; javac Client.java 