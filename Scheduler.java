//David A Foley
//OS, Prof. Gottlieb
//Lab 2
//10.8.17
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Scheduler {
	public static void main(String[] args){
		int comLine = 0;
		if(args[0].equals("--verbose"))
			comLine = 1;
		String path = System.getProperty("user.dir");//build path to input document from cwd and args[0]
		if(path.contains("\\"))//case where system separates directories with token "//"
			path+="\\";
		else
			path+="/";//case where system separates directories with token "/"
		path+=args[comLine];
		
		File input = new File (path);//get file and attempt to scan it.
		Scanner scan;
		try {
			scan = new Scanner (input);
		} catch (FileNotFoundException e) {//if file is not found, terminate the program
			System.out.println("FATAL ERROR: file "+args[comLine]+" not found at "+path);
			e.printStackTrace();
			return;
		}
		
		int numProcesses = scan.nextInt();
		pList processes = new pList(args[0].equals("--verbose"));//initialize pList, determine if verbose output is necessary
		for(int i = 0; i<numProcesses; i++){//initialize all processes
			processes.add(new Process(scan.nextInt(),scan.nextInt(),scan.nextInt(),scan.nextInt(), i));
		}
		scan.close();
		
		processes.fcfs();//run fcfs
		System.out.println();
		System.out.println();
		
		processes.roundRobin();//run rr
		System.out.println();
		System.out.println();
		
		processes.sjf();//run sjf
		System.out.println();
		System.out.println();
		
		processes.hprn();//run hprn
		
		processes.rand.close();//close the scanner in the process list which reads random numbers
	}
}

class pList{
	ArrayList<Process> untouched;//the list which holds all processes in their original state.
	//this is used to make the lists in each scheduling process
	//the scheduling methods will not change it
	
	String randFilePath;
	Scanner rand;
	File randy;
	
	boolean verbose;//true if verbose output is necessary
	
	pList(boolean b){//constructor
		untouched = new ArrayList<Process>();
		verbose = b;
		
		randFilePath=System.getProperty("user.dir");//build path to random number file
		if(randFilePath.contains("\\"))//case where system separates directories with token "\\"
			randFilePath+="\\";
		else
			randFilePath+="/";//case where system separates directories with token "/"
		randFilePath+="random-numbers.txt";
		
		randy=new File(randFilePath);
		
		try {//initialize scanner which reads random number file
			rand = new Scanner (randy);
		} catch (FileNotFoundException e) {//if file is not found, terminate the program
			System.out.println("FATAL ERROR: file random-numbers.txt not found at "+randFilePath);
			e.printStackTrace();
			return;
		}
	}//end constructor
	
	void add(Process p){//appends p to untouched
		untouched.add(p);
	}//end add
	
	void fcfs(){
		ArrayList<Process> touched = new ArrayList<Process>();//copy of process list, will be manipulated by scheduler
		
		boolean[] running=new boolean[untouched.size()];//boolean arrays store states
		boolean[] blocked=new boolean[untouched.size()];
		boolean[] unstarted=new boolean[untouched.size()];
		boolean[] terminated=new boolean[untouched.size()];
		
		PriorityQueue<Process> readyQueue=new PriorityQueue<Process>(new fcfsComparator());//Queue of ready processes
		
		for (int i=0; i<untouched.size();i++){//load all local data structures
			Process old = untouched.get(i);
			Process yaBoi = new Process(old.a, old.b, old.c, old.m, old.numInList);
			yaBoi.burstLength=0;
			touched.add(yaBoi);
			unstarted[i]=true;
			running[i]=false;
			blocked[i]=false;
			terminated[i]=false;
		}//end for
		
		int t=0;
		
		double cpuUtilization = 0;
		double ioUtilization = 0;
		
		boolean allTerminated = false;
		boolean r = false;//false when nothing is running, true when something is running
		while(!allTerminated){
			
			if(verbose==true){
				System.out.print("before cycle \t"+t+":");
				for(int i=0; i<touched.size(); i++)
					System.out.print("\t"+touched.get(i).state+"  "+touched.get(i).burstLength);
				System.out.println();
			}//end verbose output

			boolean anythingBlocked = false;//used to update IO Utilization
			for(int i=0; i<blocked.length; i++){//deal with blocked processes
				
				if(blocked[i]==true){//found blocked process
					anythingBlocked = true;
					touched.get(i).timeBlocked++;
					touched.get(i).burstLength--;
					if(touched.get(i).burstLength==0){//if io burst is done, move to ready
						touched.get(i).state="ready";
						touched.get(i).readyT=t;
						blocked[i]=false;
						readyQueue.add(touched.get(i));
					}//end case wher io burst is done
				}
			}//end blocked processes
			if(anythingBlocked)
				ioUtilization++;
			
			if(r==true)//deal with running processes
				cpuUtilization++;
			for(int i=0; i<running.length;i++){
				if(running[i]==true){//found running process
					touched.get(i).timeLeft--;//process runs
					touched.get(i).burstLength--;
					
					if(touched.get(i).timeLeft==0){//process terminates
						touched.get(i).state="terminated";
						touched.get(i).turnaroundTime=t;
						running[i]=false;
						terminated[i]=true;
						r=false;
					}
			
					else if(touched.get(i).burstLength==0){//cpu burst ends, process gets blocked
						touched.get(i).state="blocked";
						touched.get(i).burstLength=(touched.get(i).cpuBurst)*(touched.get(i).m);
						running[i]=false;
						blocked[i]=true;
						r=false;
					}
				}
			}//end dealing with running processes
			
			
			for(int i=0; i<unstarted.length; i++){//deal with unstarted processes
				if(unstarted[i]==true){//found unstarted process
					if(touched.get(i).a==t){//unstarted process has reached arrival time, enters ready queue
						touched.get(i).state="ready";
						touched.get(i).readyT=t;
						unstarted[i]=false;
						readyQueue.add(touched.get(i));
					}
				}
			}//end dealing with unstarted processes
			

			if(readyQueue.size()!=0){//deal with ready processes
				if(r==false){//case where nothing is running
					Process toRun=readyQueue.poll();//take first process from queue and run it
					readyQueue.remove(toRun);
					int toRunIndex=touched.indexOf(toRun);
					toRun.state="running";
					int cpu=randomOS(toRun.b);
					if(cpu>toRun.timeLeft)
						cpu=toRun.timeLeft;
					toRun.cpuBurst=cpu;
					toRun.burstLength=cpu;
					running[toRunIndex]=true;
					r=true;
				}
			}//end dealing with ready processes
			
			boolean maybeTerminated = terminated[0];//check if simulation is done
			int j =1;
			while(maybeTerminated==true&&j<terminated.length){
				maybeTerminated=terminated[j];
				j++;
			}
			allTerminated=maybeTerminated;
			if(!allTerminated)
				t++;
		}//end simulation while loop
		
		System.out.println("The scheduling algorithm used was FCFS");//print end data
		System.out.println();
		
		double avgTurnaround = 0;
		double avgWaitTime =0;
		for(int i=0; i<touched.size(); i++){
			Process printed = touched.get(i);
			System.out.println("Process "+printed.numInList+":");
			System.out.println("\t (A, B, C, M) = ("+printed.a+", "+printed.b+", "+printed.c+", "+printed.m+")");
			System.out.println("\t Finishing time: "+printed.turnaroundTime);
			System.out.println("\t Turnaround time: "+(printed.turnaroundTime-printed.a));
			avgTurnaround+=printed.turnaroundTime-printed.a;
			System.out.println("\t I/O time: "+printed.timeBlocked);
			printed.timeReady=printed.turnaroundTime-printed.a-printed.c-printed.timeBlocked;
			avgWaitTime+=printed.timeReady;
			System.out.println("\t Waiting time: "+(printed.timeReady));
			System.out.println();
		}
		
		System.out.println("Summary Data:");
		System.out.println("\t Finishing time: "+t);
		System.out.println("\t CPU Utilization: "+(cpuUtilization/(double)t));
		System.out.println("\t I/O Utilization: "+(ioUtilization/(double)t));
		System.out.println("\t Throughput: "+((double)touched.size()/(double)t)*100+" per hundred cycles");
		System.out.println("\t Average turnaround time: "+(avgTurnaround/(double)touched.size()));
		System.out.println("\t Average waiting time: "+(avgWaitTime/(double)touched.size()));
		
		try {//reset rand so it reads from the beginning of the file the next time a scheduler runs
			rand = new Scanner (randy);
		} catch (FileNotFoundException e) {//if file is not found, terminate the program
			System.out.println("FATAL ERROR: file random-numbers.txt not found at "+randFilePath);
			e.printStackTrace();
			return;
		}
		
	}//end fcfs
	

	void roundRobin(){
		ArrayList<Process> touched = new ArrayList<Process>();//copy of processes
		
		boolean[] running=new boolean[untouched.size()];//store states
		boolean[] blocked=new boolean[untouched.size()];
		boolean[] unstarted=new boolean[untouched.size()];
		boolean[] terminated=new boolean[untouched.size()];
		
		PriorityQueue<Process> readyQueue=new PriorityQueue<Process>(new rrComparator());//ready queue
		
		for (int i=0; i<untouched.size();i++){//fill data structures
			Process old = untouched.get(i);
			Process yaBoi = new Process(old.a, old.b, old.c, old.m, old.numInList);
			yaBoi.burstLength=0;
			touched.add(yaBoi);
			unstarted[i]=true;
			running[i]=false;
			blocked[i]=false;
			terminated[i]=false;
		}
		
		int t=0;
		
		double cpuUtilization = 0;
		double ioUtilization = 0;
		
		boolean allTerminated = false;
		boolean r = false;//false when nothing is running, true when something is running
		while(!allTerminated){//run simulation
			
			if(verbose==true){//print verbose output
				System.out.print("before cycle \t"+t+":");
				for(int i=0; i<touched.size(); i++)
					System.out.print("\t"+touched.get(i).state+"  "+touched.get(i).burstLength);
				System.out.println();
			}//end verbose output
			

			boolean anythingBlocked = false;//used to determine if ioutilization should be updated
			for(int i=0; i<blocked.length; i++){//deal with blocked processe
				if(blocked[i]==true){//found blocked process
					anythingBlocked = true;
					touched.get(i).timeBlocked++;//increment block
					touched.get(i).burstLength--;
					if(touched.get(i).burstLength==0){//finished io burst, set to ready
						touched.get(i).state="ready";
						touched.get(i).readyT=t;
						blocked[i]=false;
						readyQueue.add(touched.get(i));
					}//end case where io burst is done
				}//end case 
			}//end blocked process
			
			if(anythingBlocked)
				ioUtilization++;
		
			if(r==true)//deal with running processes
				cpuUtilization++;
			for(int i=0; i<running.length;i++){
				if(running[i]==true){//found running process
					touched.get(i).timeLeft--;//run process
					touched.get(i).burstLength--;
					touched.get(i).quantum--;

					if(touched.get(i).timeLeft==0){//process terminates
						touched.get(i).state="terminated";
						touched.get(i).turnaroundTime=t;
						running[i]=false;
						terminated[i]=true;
						r=false;
					}

					
					else if(touched.get(i).burstLength==0){//burst ends
						touched.get(i).state="blocked";
						touched.get(i).burstLength=(touched.get(i).cpuBurst)*(touched.get(i).m);
						running[i]=false;
						blocked[i]=true;
						r=false;
					}

					else if (touched.get(i).quantum==0){//process runs for its whole quantum
						touched.get(i).state="ready";
						touched.get(i).readyT=t;
						running[i]=false;
						readyQueue.add(touched.get(i));
						r=false;
					}

				}
			}//end running processes
			
			
			for(int i=0; i<unstarted.length; i++){//deal with unstarted processes
				if(unstarted[i]==true){
					if(touched.get(i).a==t){//case where process arrives
						touched.get(i).state="ready";
						touched.get(i).readyT=t;
						unstarted[i]=false;
						readyQueue.add(touched.get(i));
					}
				}
			}//end unstarted
			
			if(readyQueue.size()!=0){//deal with ready processes
				if(r==false){//case where there is no process running
					Process toRun=readyQueue.poll();//gets next process to run, run it
					readyQueue.remove(toRun);
					int toRunIndex=touched.indexOf(toRun);
					toRun.state="running";
					if(toRun.burstLength==0){
						int cpu=randomOS(toRun.b);
						if(cpu>toRun.timeLeft)
							cpu=toRun.timeLeft;
						toRun.cpuBurst=cpu;
						toRun.burstLength=cpu;
					}
					toRun.quantum=2;
					running[toRunIndex]=true;
					r=true;
				}
			}//end dealing with ready processes
			
			boolean maybeTerminated = terminated[0];//check if simulation is done
			int j =1;
			while(maybeTerminated==true&&j<terminated.length){
				maybeTerminated=terminated[j];
				j++;
			}
			allTerminated=maybeTerminated;
			if(!allTerminated)
				t++;
		}//end simulation while loop
		
		System.out.println("The scheduling algorithm used was Round Robin");
		System.out.println();
		
		double avgTurnaround = 0;
		double avgWaitTime =0;
		for(int i=0; i<touched.size(); i++){
			Process printed = touched.get(i);
			System.out.println("Process "+printed.numInList+":");
			System.out.println("\t (A, B, C, M) = ("+printed.a+", "+printed.b+", "+printed.c+", "+printed.m+")");
			System.out.println("\t Finishing time: "+printed.turnaroundTime);
			System.out.println("\t Turnaround time: "+(printed.turnaroundTime-printed.a));
			avgTurnaround+=printed.turnaroundTime-printed.a;
			System.out.println("\t I/O time: "+printed.timeBlocked);
			printed.timeReady=printed.turnaroundTime-printed.a-printed.c-printed.timeBlocked;
			avgWaitTime+=printed.timeReady;
			System.out.println("\t Waiting time: "+(printed.timeReady));
			System.out.println();
		}
		
		System.out.println("Summary Data:");
		System.out.println("\t Finishing time: "+t);
		System.out.println("\t CPU Utilization: "+(cpuUtilization/(double)t));
		System.out.println("\t I/O Utilization: "+(ioUtilization/(double)t));
		System.out.println("\t Throughput: "+((double)touched.size()/(double)t)*100+" per hundred cycles");
		System.out.println("\t Average turnaround time: "+(avgTurnaround/(double)touched.size()));
		System.out.println("\t Average waiting time: "+(avgWaitTime/(double)touched.size()));
		
		try {//reset rand so it reads from the beginning of the file the next time a scheduler runs
			rand = new Scanner (randy);
		} catch (FileNotFoundException e) {//if file is not found, terminate the program
			System.out.println("FATAL ERROR: file random-numbers.txt not found at "+randFilePath);
			e.printStackTrace();
			return;
		}
		
	}//end fcfs
	
	int randomOS(int U){
		int random = rand.nextInt();
		return 1+(random%U);
	}
	

	void sjf(){
		ArrayList<Process> touched = new ArrayList<Process>();//store copies of processes
		
		boolean[] running=new boolean[untouched.size()];//initialize data structures which store states
		boolean[] blocked=new boolean[untouched.size()];
		boolean[] unstarted=new boolean[untouched.size()];
		boolean[] terminated=new boolean[untouched.size()];
		
		PriorityQueue<Process> readyQueue=new PriorityQueue<Process>(new sjfComparator());
		
		for (int i=0; i<untouched.size();i++){//fill all data structures
			Process old = untouched.get(i);
			Process yaBoi = new Process(old.a, old.b, old.c, old.m, old.numInList);
			yaBoi.burstLength=0;
			touched.add(yaBoi);
			unstarted[i]=true;
			running[i]=false;
			blocked[i]=false;
			terminated[i]=false;
		}
		
		int t=0;
		
		double cpuUtilization = 0;
		double ioUtilization = 0;
		
		boolean allTerminated = false;
		boolean r = false;//false when nothing is running, true when something is running
		
		while(!allTerminated){
			
			if(verbose==true){//verbose output
				System.out.print("before cycle \t"+t+":");
				for(int i=0; i<touched.size(); i++)
					System.out.print("\t"+touched.get(i).state+"  "+touched.get(i).burstLength);
				System.out.println();
			}//end verbose output

			boolean anythingBlocked = false;
			for(int i=0; i<blocked.length; i++){//deal with blocked processes
				if(blocked[i]==true){
					anythingBlocked = true;
					touched.get(i).timeBlocked++;
					touched.get(i).burstLength--;
					if(touched.get(i).burstLength==0){
						touched.get(i).state="ready";
						touched.get(i).readyT=t;
						blocked[i]=false;
						readyQueue.add(touched.get(i));
					}
				}
			}//end blocked
			if(anythingBlocked)
				ioUtilization++;
		
			if(r==true)//deal with running processes
				cpuUtilization++;
			for(int i=0; i<running.length;i++){
				if(running[i]==true){//found running process
					touched.get(i).timeLeft--;//run process for 1 cycle
					touched.get(i).burstLength--;
					
					if(touched.get(i).timeLeft==0){//process terminates
						touched.get(i).state="terminated";
						touched.get(i).turnaroundTime=t;
						running[i]=false;
						terminated[i]=true;
						r=false;
					}
			
					else if(touched.get(i).burstLength==0){//cpu burst ends, process blocks
						touched.get(i).state="blocked";
						touched.get(i).burstLength=(touched.get(i).cpuBurst)*(touched.get(i).m);
						running[i]=false;
						blocked[i]=true;
						r=false;
					}
				}
			}//end dealing with running processes
			
			
			for(int i=0; i<unstarted.length; i++){//deal with unstarted process
				if(unstarted[i]==true){//found unstarted process
					if(touched.get(i).a==t){//process arrives, gets set to ready
						touched.get(i).state="ready";
						touched.get(i).readyT=t;
						unstarted[i]=false;
						readyQueue.add(touched.get(i));
					}
				}
			}//end dealing with unstarted processes
			
			if(readyQueue.size()!=0){//deal with ready processes
				if(r==false){//case where there's no process running
					Process toRun=readyQueue.poll();//gets next process, sets it to run
					readyQueue.remove(toRun);
					int toRunIndex=touched.indexOf(toRun);
					toRun.state="running";
					int cpu=randomOS(toRun.b);
					if(cpu>toRun.timeLeft)
						cpu=toRun.timeLeft;
					toRun.cpuBurst=cpu;
					toRun.burstLength=cpu;
					running[toRunIndex]=true;
					r=true;
				}
			}//end dealing with ready 
			
			boolean maybeTerminated = terminated[0];//check if simulation is over
			int j =1;
			while(maybeTerminated==true&&j<terminated.length){
				maybeTerminated=terminated[j];
				j++;
			}
			allTerminated=maybeTerminated;
			if(!allTerminated)
				t++;
		}//end simulation while loop
		
		System.out.println("The scheduling algorithm used was SJF");
		System.out.println();
		
		double avgTurnaround = 0;
		double avgWaitTime =0;
		for(int i=0; i<touched.size(); i++){
			Process printed = touched.get(i);
			System.out.println("Process "+printed.numInList+":");
			System.out.println("\t (A, B, C, M) = ("+printed.a+", "+printed.b+", "+printed.c+", "+printed.m+")");
			System.out.println("\t Finishing time: "+printed.turnaroundTime);
			System.out.println("\t Turnaround time: "+(printed.turnaroundTime-printed.a));
			avgTurnaround+=printed.turnaroundTime-printed.a;
			System.out.println("\t I/O time: "+printed.timeBlocked);
			printed.timeReady=printed.turnaroundTime-printed.a-printed.c-printed.timeBlocked;
			avgWaitTime+=printed.timeReady;
			System.out.println("\t Waiting time: "+(printed.timeReady));
			System.out.println();
		}
		
		System.out.println("Summary Data:");
		System.out.println("\t Finishing time: "+t);
		System.out.println("\t CPU Utilization: "+(cpuUtilization/(double)t));
		System.out.println("\t I/O Utilization: "+(ioUtilization/(double)t));
		System.out.println("\t Throughput: "+((double)touched.size()/(double)t)*100+" per hundred cycles");
		System.out.println("\t Average turnaround time: "+(avgTurnaround/(double)touched.size()));
		System.out.println("\t Average waiting time: "+(avgWaitTime/(double)touched.size()));
		
		try {//reset rand so it reads from the beginning of the file the next time a scheduler runs
			rand = new Scanner (randy);
		} catch (FileNotFoundException e) {//if file is not found, terminate the program
			System.out.println("FATAL ERROR: file random-numbers.txt not found at "+randFilePath);
			e.printStackTrace();
			return;
		}
		
	}
	
	void hprn(){
		ArrayList<Process> touched = new ArrayList<Process>();//initialize data structures to store processes and states
		boolean[] running=new boolean[untouched.size()];
		boolean[] blocked=new boolean[untouched.size()];
		boolean[] ready=new boolean[untouched.size()];
		boolean[] unstarted=new boolean[untouched.size()];
		boolean[] terminated=new boolean[untouched.size()];
		
		for (int i=0; i<untouched.size();i++){//fill data structures
			Process old = untouched.get(i);
			Process yaBoi = new Process(old.a, old.b, old.c, old.m, old.numInList);
			yaBoi.burstLength=0;
			touched.add(yaBoi);
			unstarted[i]=true;
			running[i]=false;
			blocked[i]=false;
			ready[i]=false;
			terminated[i]=false;
		}
		
		int t=0;
		
		double cpuUtilization = 0;
		double ioUtilization = 0;
		
		boolean allTerminated = false;
		boolean r = false;//false when nothing is running, true when something is running
		
		while(!allTerminated){
			
			if(verbose==true){//print verbose output
				System.out.print("before cycle \t"+t+":");
				for(int i=0; i<touched.size(); i++)
					System.out.print("\t"+touched.get(i).state+"  "+touched.get(i).burstLength);
				System.out.println();
			}//end printing verbose output

			boolean anythingBlocked = false;
			for(int i=0; i<blocked.length; i++){//deal with blocked processes
				if(blocked[i]==true){//found blocked processes
					anythingBlocked = true;
					touched.get(i).timeBlocked++;
					touched.get(i).burstLength--;
					
					if(touched.get(i).burstLength==0){//case where process gets unblocked
						touched.get(i).state="ready";
						touched.get(i).readyT=t;
						touched.get(i).r = ((double)t-(double)touched.get(i).a)/(double)Math.max(1, (touched.get(i).c-touched.get(i).timeLeft));//set r
						blocked[i]=false;
						ready[i]=true;
					}
				}
			}//end dealing with blocked processes
			if(anythingBlocked)
				ioUtilization++;
		
			if(r==true)//deal with running processes
				cpuUtilization++;
			for(int i=0; i<running.length;i++){
				if(running[i]==true){//found running process
					touched.get(i).timeLeft--;
					touched.get(i).burstLength--;
					
					if(touched.get(i).timeLeft==0){//process terminates
						touched.get(i).state="terminated";
						touched.get(i).turnaroundTime=t;
						running[i]=false;
						terminated[i]=true;
						r=false;
					}
			
					else if(touched.get(i).burstLength==0){//process blocks
						touched.get(i).state="blocked";
						touched.get(i).burstLength=(touched.get(i).cpuBurst)*(touched.get(i).m);
						running[i]=false;
						blocked[i]=true;
						r=false;
					}
				}
			}//end dealing with running processes
			
			
			for(int i=0; i<unstarted.length; i++){//deal with unstarted processes
				if(unstarted[i]==true){
					if(touched.get(i).a==t){//process arrives, set to ready
						touched.get(i).state="ready";
						touched.get(i).readyT=t;
						touched.get(i).r = ((double)t-(double)touched.get(i).a)/(double)Math.max(1, (touched.get(i).c-touched.get(i).timeLeft));//set r
						unstarted[i]=false;
						ready[i]=true;
					}
				}
			}//end unstarted
			
			for(int i=0; i<ready.length; i++){//increment all ready processes
				if(ready[i]){
					Process now = touched.get(i);
					now.timeReady++;
					now.r= ((double)t-(double)now.a)/(double)Math.max(1, (now.c-now.timeLeft));//set r to latest value
				}
			}
			
			if(!r){//case where nothing is currently running 
				PriorityQueue<Process> readyQueue = new PriorityQueue<Process>(new hprnComparator());
				for(int i=0; i<ready.length; i++){//build heap
					if(ready[i]){
						readyQueue.add(touched.get(i));
					}
				}
				if(readyQueue.size()!=0){//case where something is available to run
					Process toRun = readyQueue.poll();//get next ready process to run
					readyQueue.remove(toRun);
					int toRunIndex=touched.indexOf(toRun);
					toRun.state="running";
					int cpu=randomOS(toRun.b);
					if(cpu>toRun.timeLeft)
						cpu=toRun.timeLeft;
					toRun.cpuBurst=cpu;
					toRun.burstLength=cpu;
					ready[toRunIndex]=false;
					running[toRunIndex]=true;
					r=true;
				}
			}//end dealing with ready processes
			
			boolean maybeTerminated = terminated[0];//check if simulation is over
			int j =1;
			while(maybeTerminated==true&&j<terminated.length){
				maybeTerminated=terminated[j];
				j++;
			}
			allTerminated=maybeTerminated;
			if(!allTerminated)
				t++;
		}//end simulation while loop
		
		System.out.println("The scheduling algorithm used was HPRN");
		System.out.println();
		
		double avgTurnaround = 0;
		double avgWaitTime =0;
		for(int i=0; i<touched.size(); i++){
			Process printed = touched.get(i);
			System.out.println("Process "+printed.numInList+":");
			System.out.println("\t (A, B, C, M) = ("+printed.a+", "+printed.b+", "+printed.c+", "+printed.m+")");
			System.out.println("\t Finishing time: "+printed.turnaroundTime);
			System.out.println("\t Turnaround time: "+(printed.turnaroundTime-printed.a));
			avgTurnaround+=printed.turnaroundTime-printed.a;
			System.out.println("\t I/O time: "+printed.timeBlocked);
			printed.timeReady=printed.turnaroundTime-printed.a-printed.c-printed.timeBlocked;
			avgWaitTime+=printed.timeReady;
			System.out.println("\t Waiting time: "+(printed.timeReady));
			System.out.println();
		}
		
		System.out.println("Summary Data:");
		System.out.println("\t Finishing time: "+t);
		System.out.println("\t CPU Utilization: "+(cpuUtilization/(double)t));
		System.out.println("\t I/O Utilization: "+(ioUtilization/(double)t));
		System.out.println("\t Throughput: "+((double)touched.size()/(double)t)*100+" per hundred cycles");
		System.out.println("\t Average turnaround time: "+(avgTurnaround/(double)touched.size()));
		System.out.println("\t Average waiting time: "+(avgWaitTime/(double)touched.size()));
		
		try {//reset rand so it reads from the beginning of the file the next time a scheduler runs
			rand = new Scanner (randy);
		} catch (FileNotFoundException e) {//if file is not found, terminate the program
			System.out.println("FATAL ERROR: file random-numbers.txt not found at "+randFilePath);
			e.printStackTrace();
			return;
		}
		
	}//end hprn
}


class Process{
	String state;
	int a;
	int b;
	int c;
	int m;
	int numInList;
	int burstLength;
	int timeLeft;
	int timeBlocked;
	int timeReady;
	int turnaroundTime;
	int cpuBurst;
	int readyT;
	double r;
	
	int quantum;
	Process(int A, int B, int C, int M, int i){
		state = "unstarted";
		a=A;
		b=B;
		c=C;
		m=M;
		numInList=i;
		timeLeft = c;
		timeBlocked = 0;
		timeReady = 0;
		burstLength = 0;
	}
	
}
class fcfsComparator implements Comparator<Process>{//sort criteria for ready processes in fcfs
	public int compare(Process p1, Process p2){
		
		if(p1.readyT>p2.readyT)//sort first by time put in the ready queue
			return 1;
		if(p1.readyT<p2.readyT)
			return -1;
		
		if(p1.a>p2.a)//if placed in queue at same time, sort by arrival time
			return 1;
		if(p1.a<p2.a)
			return-1;
		
		else{//if arrive at same time, sort by order of appearance in list
			if(p1.numInList>p2.numInList)
				return 1;
			else
				return -1;
		}
	}
}//end sort criteria for fcfs ready processes

class rrComparator implements Comparator<Process>{//sort criteria for ready processes in round robin scheduler
	public int compare(Process p1, Process p2){
		if(p1.readyT>p2.readyT)//sort first by time put in the ready queue
			return 1;
		if(p1.readyT<p2.readyT)
			return -1;
		
		if(p1.a>p2.a)//if placed in queue at same time, sort by arrival time
			return 1;
		if(p1.a<p2.a)
			return-1;
		
		else{//if arrive at same time, sort by order of appearance in list
			if(p1.numInList>p2.numInList)
				return 1;
			else
				return -1;
		}
	}
}//end sort criteria for ready processes in round robin scheduler

class sjfComparator implements Comparator<Process>{//sort criteria for ready processes in sjf scheduler
	public int compare(Process p1, Process p2){
		if(p1.timeLeft>p2.timeLeft)//sort first by amount of processing time left
			return 1;
		if(p1.timeLeft<p2.timeLeft)
			return -1;
		
		if(p1.a>p2.a)//if they have the same time left, sort by arrival time
			return 1;
		if(p1.a<p2.a)
			return-1;
		
		else{//if arrive at same time, sort by order of appearance in list
			if(p1.numInList>p2.numInList)
				return 1;
			else
				return -1;
		}
	}
}//end sort criteria for ready processes in sjf scheduler

class hprnComparator implements Comparator<Process>{//sort criteria for ready processes in hprn  scheduler
	public int compare(Process p1, Process p2){
		if(p1.r>p2.r)//sort first by penalty ratio
			return -1;
		if(p1.r<p2.r)
			return 1;

		if(p1.a>p2.a)//if they have the same penalty ratio, sort by arrival time
			return 1;
		if(p1.a<p2.a)
			return-1;
		
		else{//if arrive at same time, sort by order of appearance in list
			if(p1.numInList>p2.numInList)
				return 1;
			else
				return -1;
		}
	}
}//end sort criteria for ready processes in hprn scheduler
