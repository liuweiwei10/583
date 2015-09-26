import java.util.HashSet;
import java.util.Set;

public class TreeNode {
	String value;
	TreeNode left;
	TreeNode right;
	Set<String> postingList;
    
	public TreeNode(String value) {
		this.value = value;
		this.left = null;
		this.right = null;
		this.postingList = new HashSet<String>();
	}
}
