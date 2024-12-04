import java.util.*;

public class Solution {
    public int[] solution(int []arr) {
        int[] answer = {};
        int cnt = -1;
        List<Integer> list = new ArrayList<>();
        for(int n : arr){
            if(n!=cnt) list.add(n);
            cnt = n;
        }


        return list.stream().distinct().mapToInt(Integer::intValue).toArray();
    }
}