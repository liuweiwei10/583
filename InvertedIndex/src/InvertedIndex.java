import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


public class InvertedIndex {
	private static String fileName = "input.txt"; 
	private static String query;
	private static Map<String, Set<String>> invertedIndex;
	
	public static void main(String[] args) {
		
		//obtain the input filename and query
		if(args.length == 0) {
			System.out.println("Please run with query");
			return;
		} else if (args.length == 1) {
			query = args[0];
			//System.out.println(query);
		} else {
			fileName = args[0];
			query = args[1];
		}				
		
		
		//build invertd index
		invertedIndex = buildInvertedIndex(fileName);
				
	    //build tree for query
	    TreeNode root = buildTreeFromQuery(query, invertedIndex);
	    
	    if(root == null) {
	    	return;
	    }
	    
	    //Traverse the tree to get result;
	    Set<String> resultSet = traverseQueryTreeToGetResult(root);
	    
	    //Sort the results, and output them.
	    int count;
	    if(resultSet == null|| resultSet.size() == 0) {
	    	count = 0;
	    } else {
	    	count = resultSet.size();
	    }
	    if(count < 2) {
		    System.out.println(count + " result found!");
	    }else {
		    System.out.println(count + " results found!");
	    }
	    
        if(resultSet!=null && !resultSet.isEmpty()) {
        	List<String> resultList = new ArrayList<String>(resultSet);
        	Collections.sort(resultList);
        	outputList(resultList);
        }
	}	
	
	
	/***
	 * Traverse the query tree and perform the basic AND/OR operations in post-order.
	 * @param root
	 * @return result docIDs
	 */
	public static Set<String> traverseQueryTreeToGetResult(TreeNode root) {
		Set<String> results = null;		
		//post-order traverse 
		if(root != null) {
			traverseQueryTreeToGetResult(root.left);
			traverseQueryTreeToGetResult(root.right);
	                
            //perform the query of this node
            if(root.value.equals("AND")) {
            	Set<String> postingList = performBasicAND(root.left.postingList, root.right.postingList);
            	root.postingList= postingList;
            	results = root.postingList;
            } else if(root.value.equals("OR")) {
            	Set<String> postingList = performBasicOR(root.left.postingList, root.right.postingList);
            	root.postingList= postingList;
            	results = root.postingList;
            } else {
            	results = root.postingList;
            }
		} 			
		return results;
	}
	
	/***
	 * validate the query in advance: excludes cases such as "x y OR", "x OR AND"
	 * @param query
	 * @return
	 */
	public static boolean validateQuery(String[] query) {
	   	boolean result = true;
	   	boolean isOperator = false;
		for(int i = 0; i < query.length; i++) {
	   		if(query[i].equals("(")||query[i].equals(")"))
	   			continue;
	   		else {
	   			if(isOperation(query[i]) != isOperator) {
	   				result = false;
	   			    break;
	   			}
	   			isOperator = !isOperator;
	   		}
	   	}
		return result;
	}
	
	
	/***
	 * 
	 * @param query
	 * @param index
	 * @return
	 */
	public static TreeNode buildTreeFromQuery(String query, Map<String, Set<String>> index) {
	    TreeNode root = null;
	    
	    //parse the query 
	    String[] op =  parseQuery(query);
	    if(!validateQuery(op)) {
	    	System.out.println("invalid query!");
			return null;
	    }
	    
	    //two stacks, one for the treenodes and another for operations (including "AND", "OR", and parentheses)   
		Stack<TreeNode> treeNodeStack = new Stack<TreeNode>();
		Stack<String> operationsStack = new Stack<String>();		
		
		if(op.length == 1) {
			//handle the special case that the query only has a single keyword
			if(isOperation(op[0])) {
				System.out.println("invalid query!");
		    	return null;
			} else {
				TreeNode singleNode = new TreeNode(op[0]);
    			if(index.get(op[0])!= null) {
	    			singleNode.postingList = index.get(op[0]);
    			}
                return singleNode;		
			}
		}else if(op.length < 3) {
	    	System.out.println("invalid query!");
	    	return null;
	    } else {
	    	for(int i =0 ; i < op.length; i++) {
	    		if(isOperation(op[i])) {
	    			//handle "AND", "OR", "(" , ")"
	    			if(operationsStack.isEmpty()) {    				
	    				if(op[i].equals(")")) {
	    					// no match for ")" 
	    			    	System.out.println("invalid query! please check parentheses.");
	    			    	return null;
	    				} 
	    				operationsStack.push(op[i]);    				
	    			} else {
	    				if(op[i].equals("AND")) {
	    					//deal with "and"
	    					String lastOp = operationsStack.peek();
	    					if(lastOp.equals("OR")) {
	    						// Parentheses must be used to specify the execution order of "AND" and "OR"
	    						System.out.println("invalid query! please use parentheses to specify the order of \"OR\" and \"AND\" ");
		    			    	return null;
	    					} else if(lastOp.equals("AND")) {
	    						operationsStack.push(op[i]);
	    						TreeNode node = buildSubtree(treeNodeStack, operationsStack);
	    						if(node == null) {
		    						System.out.println("invalid query!");
		    						return null;
	    						} else {
	    							treeNodeStack.push(node);
	    						}	    						
	    					} else if(lastOp.equals("(")) {
	    						operationsStack.push(op[i]);
	    					} else {
	    						System.out.println("invalid query!");
	    						return null;
	    					}	    					
	    				} else if(op[i].equals("OR")) {
	    					//deal with "OR"
	    					String lastOp = operationsStack.peek();
	      					if(lastOp.equals("AND")) {
	    						// Parentheses must be used to specify the execution order of "AND" and "OR"
	    						System.out.println("invalid query! please use parentheses to specify the order of \"OR\" and \"AND\" ");
		    			    	return null;
	    					} else if(lastOp.equals("OR")) {
	    						operationsStack.push(op[i]);
	    						TreeNode node = buildSubtree(treeNodeStack, operationsStack);
	    						if(node == null) {
		    						System.out.println("invalid query!");
		    						return null;
	    						} else {
	    							treeNodeStack.push(node);
	    						}	    						
	    					} else if(lastOp.equals("(")) {
	    						operationsStack.push(op[i]);
	    					} else {
	    						System.out.println("invalid query!");
	    						return null;
	    					}
	    				} else if(op[i].equals("(")) {
	    					//deal with "("
	    					operationsStack.push(op[i]);	    						    					
	    				} else if(op[i].equals(")")) {
	    					//deal with ")"
	    					String lastOp = operationsStack.peek();
	    					if(lastOp.equals("(")) {
	    						operationsStack.pop();
	    					}else if(lastOp.equals("AND") || lastOp.equals("OR")) {
	    						String operation = operationsStack.pop();
	    						if(operationsStack.isEmpty()) {
	    							System.out.println("invalid query!");
	        						return null;
	    						} else {
	    							String matchingParenthese = operationsStack.pop();
	    							if(!matchingParenthese.equals("(")) {
	    								System.out.println("invalid query!");
		        						return null;
	    							}                                  	    							
    						    }
	    						operationsStack.push(operation);
	    						TreeNode node = buildSubtree(treeNodeStack, operationsStack);
	    						if(node == null) {
		    						System.out.println("invalid query!");
		    						return null;
	    						} else {
	    							treeNodeStack.push(node);
	    						}	
	    						
	    					} else {
	    						System.out.println("invalid query!");
	    						return null;
	    					}
	    				} else {
	    					System.out.println("invalid query!");
    						return null;
	    				}   			 	    				
	    			}
	    		} else {
	    			//handle the case that a keyword is encountered
	    			TreeNode node = new TreeNode(op[i]);
	    			if(index.get(op[i])!= null) {
		    			node.postingList = index.get(op[i]);
	    			}
	    			treeNodeStack.push(node);
	    		}
	    	}
	    	
	    	//take care of the final states of the stacks
	    	if(operationsStack.isEmpty()) {
	    		if(treeNodeStack.size() != 1) {
	    			System.out.println("invalid query!");
					return null;
	    		} else {
	    			root = treeNodeStack.pop();	    			
	    		}
	    	} else {
	    		if(operationsStack.size()!=1 || treeNodeStack.size()!= 2) {
	    			System.out.println("invalid query!");
					return null;
	    		} else {
	    			String operation = operationsStack.peek();
	    			if(operation.equals("AND") || operation.equals("OR")) {
	    				root = buildSubtree(treeNodeStack,operationsStack);
	    				if(root == null) {
    						System.out.println("invalid query!");
						}
	    			}else {
	    				System.out.println("invalid query!");
						return null;
	    			}
	    		}
	    	}
	    	
	    }	    
	    return root;
	}
	
	/***
	 * parse the query into a string array
	 * @param query
	 * @return
	 */
	public static String[] parseQuery(String query) {
		List<String> list =  new ArrayList<String>();
		
		//first split by spaces
		//String[] strs =  query.split("\\s+");
		
		String[] strs = query.split("((?<=\\()|(?=\\()|(?<=\\))|(?=\\))|\\s+)");
		
		//deal with extra spaces
		for(int i = 0; i < strs.length; i++) {
			if(strs[i].trim().isEmpty()) {
				continue;
			} else {
				list.add(strs[i].trim());
			}
		}
		
		return list.toArray(new String[list.size()]);
	}
	
	
	/***
	 * print stack
	 * @param stack
	 */
	public static void printStack(Stack<TreeNode> stack) {
		while(!stack.isEmpty()) {
			System.out.println(stack.pop().value);
		}
	}
	
	/***
	 * get the operation from operation stack, get two treenodes from the treeNode stack, build a subtree
	 * @param stack
	 * @param opStack
	 * @return the root of the subtree
	 */
	public static TreeNode buildSubtree(Stack<TreeNode> stack, Stack<String> opStack) {
		String operation;
		TreeNode node1;
		TreeNode node2;
		
		if(!opStack.isEmpty()) {
		     operation = opStack.pop();
		} else {
			return null;
		}
		if(!stack.isEmpty()) {
			node1 = stack.pop();
		} else {
			return null;
		}
		
		if(!stack.isEmpty()) {			
		     node2 = stack.pop();
		} else {
			return null;
		}
		
		TreeNode  node = new TreeNode(operation);
		node.right = node1;
		node.left = node2;
		return node;		
	}
	
	/***
	 * Check if a string is a operation or not
	 * @param str
	 * @return true if yes, otherwise no
	 */
	public static boolean isOperation(String str) {
		if(str.equals("AND") || str.equals("OR") || str.equals("(") || str.equals(")"))
			return true;
		else return false;
	}
	
	/***
	 * print a string list
	 * @param list
	 */
	public static void outputList(List<String> list) {
		for(String str: list) {
			System.out.print(str + " ");
		}
		System.out.print("\n");
	}
	
	
	/****
	 * Basic "AND" operation, i.e. get the intersection of two posting list
	 * @param postingList1
	 * @param postingList2
	 * @return
	 */
	public static Set<String> performBasicAND(Set<String> postingList1, Set<String> postingList2) {
	    if(postingList1.isEmpty() || postingList2.isEmpty()) {
	        return new HashSet<String>();	  
	    }
        Set<String> intersection = new HashSet<String>(postingList1);
        intersection.retainAll(postingList2);
		return intersection;
	}
	
	/****
	 * Basic "OR" operation, i.e. get the union of two posting list
	 * @param postingList1
	 * @param postingList2
	 * @return
	 */
	public static Set<String> performBasicOR(Set<String> postingList1, Set<String> postingList2) {
	    if(postingList1.isEmpty() && postingList2.isEmpty()) {
	        return new HashSet<String>();	  
	    }
        Set<String> union = new HashSet<String>(postingList1);	
        union.addAll(postingList2);
		return union;
	}
	
	
    /***
     * build inverted index for docs
     * @param filename
     * @return
     */
	private static Map<String, Set<String>> buildInvertedIndex(String filename) {
		
	    Map<String, Set<String>> postingLists = new HashMap<String, Set<String>>();
		BufferedReader br = null;
		try {
			String line;
			String[] tokens;
			br = new BufferedReader(new FileReader(fileName));

			//process the input file line by line and build inverted index.
			while ((line = br.readLine()) != null) {
				tokens = line.split(" ");
				String docID = tokens[0];
				for(int i=1; i< tokens.length; i++) {
					if(postingLists.containsKey(tokens[i])) {
						Set<String> docs = postingLists.get(tokens[i]);
						docs.add(docID);
						postingLists.put(tokens[i], docs);
					} else {
						Set<String> docs = new HashSet<String>();
						docs.add(docID);
						postingLists.put(tokens[i], docs);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return postingLists;
	}
	
}
