import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class Donors {
	class medianStream{
	   /** This class is used to compute the running median
		 * of the money from the same zip to the same recipient.
		 * Using a minHeap and a maxHeap to fast compute the 
		 * median in O(1). To maintain the structure, insert
		 * element is O(logN). */
		PriorityQueue<Integer> minHeap;
		PriorityQueue<Integer> maxHeap;
		int transactions;
		int amount;
		public medianStream(){
			minHeap=new PriorityQueue<>();
			maxHeap=new PriorityQueue<>();
			transactions=0;
			amount=0;
		}
		public int getMedian(){
			if(transactions%2==1)
				return minHeap.peek();
			return (int)Math.round((minHeap.peek()-maxHeap.peek())/2.0);
		}
		public void insert(int num){
			minHeap.add(num);
			maxHeap.add(-minHeap.poll());
			if(minHeap.size()<maxHeap.size())
				minHeap.add(-maxHeap.poll());
			transactions++;
			amount+=num;
		}
	}
	
	class Recipient{
	   /** The recipient class has its ID and two maps.
		 * dollarByDate has date as key and a list of transactions
		 * on that day as value. 
		 * dollarByZip has zip code as key and its median calculator
		 * medianStream as value.
		 * dollarByDate is a TreeMap for sorting.
		 * */
		String CMTE_ID;
		TreeMap<String, ArrayList<Integer>> dollarByDate;
		HashMap<String, medianStream> dollarByZip;
		public Recipient(String s){
			CMTE_ID=s;
			dollarByDate=new TreeMap<>(dateComparator);
			dollarByZip=new HashMap<>();
		}
		/** sort by date */
		Comparator<String> dateComparator=new Comparator<String>(){
			public int compare(String s1, String s2){
				int tmp=s1.substring(4, 8).compareTo(s2.substring(4, 8));
				if(tmp!=0) return tmp;
				return s1.substring(0, 4).compareTo(s2.substring(0, 4));
			}
		};
		/** receive donation info, record the donation by zip and date if valid */
		public void getDonations(String zip, String date, int amount, 
				boolean computeZip, boolean computeDate){
			if(computeZip){
				if(!dollarByZip.containsKey(zip))
					dollarByZip.put(zip, new medianStream());
				dollarByZip.get(zip).insert(amount);
			}
			if(computeDate){
				if(!dollarByDate.containsKey(date))
					dollarByDate.put(date, new ArrayList<Integer>());
				dollarByDate.get(date).add(amount);
			}
		}
	}
	
	/** A TreeMap sort the recipients by IDs */
	TreeMap<String, Recipient> map;
	
	public Donors(){
		map=new TreeMap<>();
	}
	
	/** Input itcont from file, compute the running median and output to zip file */
	public void dataInput(String filename1, String filename2){
		try{
			BufferedReader br=new BufferedReader(new FileReader(filename1));
			BufferedWriter bw=new BufferedWriter(new FileWriter(filename2));
			String line;
			while((line=br.readLine())!=null){
				boolean computeZip=true;
				boolean computeDate=true;
				String[] sarr=line.split("\\|");
				
				// CMTE_ID, skip if empty
				String cmteID=sarr[0];
				if(cmteID.length()==0) continue;
				
				// zip code, skip for zip record if invalid
				// keep first 5 digits
				String zip=sarr[10];
				if(zip.length()<5) computeZip=false;
				else if(zip.length()>5) zip=zip.substring(0, 5);
				
				// date, skip for date record if invalid
				String date=sarr[13];
				if(!isValidDate(date)) computeDate=false;
				
				// donation amount, skip if empty
				String amt=sarr[14];
				if(amt.length()==0) continue;
				
				// other, skip if not empty
				if(sarr[15].length()!=0) continue;
				
				addInData(cmteID, zip, date, amt, computeZip, computeDate);
				
				if(computeZip){
					medianStream ms=map.get(cmteID).dollarByZip.get(zip);
					String outStr=cmteID+"|"+zip+"|"+ms.getMedian()+"|"+ms.transactions+"|"+ms.amount+"\n";
					bw.write(outStr);
				}
			}
			br.close();
			bw.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/** compute and write the median by date to file. Two methods are used. Direct
	 * sort and find median, O(NlogN). QuickSelect, average O(N), worst O(N^2). In
	 * test from FEC data, since for a particular recipient, the transactions per day
	 * is not large, direct sort performs better and is used by default. But quickSelect
	 * method is also provided. */
	public void outputDate(String filename){
		try{
			BufferedWriter bw=new BufferedWriter(new FileWriter(filename));
			for(Recipient rp:map.values()){
				for(Map.Entry<String, ArrayList<Integer>> entry:rp.dollarByDate.entrySet()){
					String date=entry.getKey();
					List<Integer> ls=entry.getValue();
					int median=0, sum=0, size=ls.size();;
					//median=computeMedian(ls); // quickSelect
					Collections.sort(ls); // direct sort
					if(size%2==0)
						median=(int)Math.round((ls.get(size/2-1)+ls.get(size/2))/2.0);
					else
						median=ls.get(size/2);
					for(int i:ls) sum+=i;
					String outStr=rp.CMTE_ID+"|"+date+"|"+median+"|"+size+"|"+sum+"\n";
					bw.write(outStr);
				}
			}
			bw.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/** blocks of implementation of quickSelect to get median */
	/*private int computeMedian(List<Integer> list){
		if(list.size()==1) return list.get(0);
		int n=list.size(), k=n/2;
		if(n/2==1) return quickSelect(list, k, 0, n-1);
		return (int)Math.round((quickSelect(list, k-1, 0, n-1)+quickSelect(list, k, 0, n-1))/2.0);
	}
	private int quickSelect(List<Integer> list, int k, int l, int r){
		int pos=randomPartition(list, l, r);
		if(pos-l==k-1) return list.get(pos);
		if(pos-l>k-1) return quickSelect(list, k, l, pos-1);
		return quickSelect(list, k-pos+l-1, pos+1, r);
	}
	private int randomPartition(List<Integer> list, int l, int r){
		int n=r-l+1;
		int pivot=(int)(Math.random())%n;
		swap(list, l+pivot, r);
		return partition(list, l, r);
	}
	private int partition(List<Integer> list, int l, int r){
		int x = list.get(r), i = l;
        for (int j = l; j <= r - 1; j++){
            if (list.get(j) <= x){
                swap(list, i, j);
                i++;
            }
        }
        swap(list, i, r);
        return i;
	}
	private void swap(List<Integer> list, int i, int j){
		int tmp=list.get(i);
		list.set(i, list.get(j));
		list.set(j, tmp);
	}*/
	
	private void addInData(String cmteID, String zip, String date, 
			String amt, boolean computeZip, boolean computeDate){
		if(!map.containsKey(cmteID))
			map.put(cmteID, new Recipient(cmteID));
		Recipient rp=map.get(cmteID);
		rp.getDonations(zip, date, Integer.parseInt(amt), computeZip, computeDate);
	}
	
	/** roughly check if date is valid. */
	private boolean isValidDate(String s){
		if(s.length()!=8) return false;
		for(char c:s.toCharArray()){
			if(!Character.isDigit(c)) return false;
		}
		if(Integer.parseInt(s.substring(0, 2))>12 || Integer.parseInt(s.substring(2, 4))>31 ||
				Integer.parseInt(s.substring(4))>2017)
			return false;
		return true;
	}
	

	public static void main(String[] args) {
		String inputName=args[0];
		String zipName=args[1];
		String dateName=args[2];
		Donors donors=new Donors();
		donors.dataInput(inputName, zipName);
		donors.outputDate(dateName);
	}

}
