import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;

public class SymbolTableVisitor extends CLangDefaultVisitor {

    Vector<String> _data;
    Vector<String> _text;
    int stackIndex = 0;
    
    public static class SymbolTableEntry {
        public String name;
        public String type;
        public int offset;

        public SymbolTableEntry(String name, String type, int offset)
        {
            this.name = name;
            this.type = type;
            this.offset = offset;
        }
    }

    public static class func{
       public int arg;
       public int counter;
       public String id;
       public String type;
       public Vector<SymbolTableEntry> argList;
       public  func(String _id, String t){
            id= _id;
            counter=0;
            type=t;
            argList= new Vector<SymbolTableEntry>();
            arg=0;
        }
        public void  addPram(SymbolTableEntry e){
            argList.add(arg, e);
            //System.err.println(String.format("in parms %s: %s",e.name,arg));
            arg++;
        }
        public SymbolTableEntry getParm(int index){
            //System.err.println(String.format("index parm: %s",index));
           if(index>=arg){
               return null;
           }
            SymbolTableEntry e=null;
            
             e= argList.get(index);
            
            return e;

        }
    }
    int loopCounter=0;
    func curr=null;
    int index =0;
    String return_val = "void";
    Vector<HashMap<String, SymbolTableEntry>> symbols;
    HashMap<String, func> funcMap = new HashMap<String, func>();

    public SymbolTableVisitor() {
        this._text = new Vector<>();
        this._data = new Vector<>();

        symbols = new Vector<HashMap<String, SymbolTableEntry>>();

        symbols.add(0, new HashMap<String, SymbolTableEntry>());
    }

    public SymbolTableEntry resolve(String s) {
        //System.err.println(String.format("in resolve"));
        int temp = index;
        
        while(temp>=0){
            SymbolTableEntry o=  symbols.get(temp).get(s);
            if(o!=null){
                return o;
            } 
            temp--;
        }
        return null;
    }

    public void put(SymbolTableEntry s)
    {
        symbols.get(index).put(s.name, s);
    }

    public void putFunc(func temp){
        funcMap.put(temp.id, temp);
    }
    public func resolvFunc(String id){
        func t= funcMap.get(id);
        return t;
    }
    


    @Override
    public Object visit(ASTparamDef node, Object data) {
        //System.err.println(String.format("in"));
        SymbolTableEntry e = resolve(node.firstToken.next.image);
        if(e!=null){
            System.err.println(String.format("Variable %s is already defined at %d : %d",
            node.firstToken.image,
            node.firstToken.beginLine,
            node.firstToken.beginColumn));
            System.exit(-1);
            //error func use arg name that used in global already + exit
           // System.err.println(String.format("Variable %s is already"));
        }
        //System.err.println(String.format("out"));
        e = new SymbolTableEntry(node.firstToken.next.image, node.firstToken.image, this.stackIndex);
        boolean isInt= node.firstToken.image.equals("int");
      
        if(isInt){this.stackIndex += 4;} 
        else this.stackIndex ++;
       //asmbli add to data
        curr.addPram(e);
        Object res = super.visit(node, data);        
        return res;
    }

    @Override
    public Object visit(ASTvarDefineDef node, Object data) {
       // System.err.println(String.format("in VAR DEFINEDEF"));
        SymbolTableEntry temp = resolve(node.firstToken.next.image);
        func temp2= resolvFunc(node.firstToken.next.image);
            if (temp != null)
            {
                System.err.println(String.format("Variable %s is already defined at %d : %d",
                                                    node.firstToken.next.image,
                                                    node.firstToken.beginLine,
                                                    node.firstToken.beginColumn));
                System.exit(-1);
            }
            if (temp2 != null)
            {
                System.err.println(String.format("Variable %s is already defined as function at %d : %d",
                                                    node.firstToken.next.image,
                                                    node.firstToken.beginLine,
                                                    node.firstToken.beginColumn));
                System.exit(-1);
            }
        boolean isInt = node.firstToken.image.equals("int");
        if (isInt)
            this.stackIndex+=4;
        else
            this.stackIndex++;
            //System.err.println(String.format("in var="));
        SymbolTableEntry e = new SymbolTableEntry(node.firstToken.next.image, node.firstToken.image, this.stackIndex);
        
        if(node.children!= null){
            if (node.children.length > 0)
            {
            return_val= e.type;
            data = node.children[0].jjtAccept(this, data);
            _text.add("pop rax");
            _text.add(String.format("mov %s [rbp - %d], %s", isInt ? "dword" : "byte", e.offset, isInt ? "eax" : "al"));
            }
        }
        put(e);
        return_val="void";
        return data;
    }

    @Override
    public Object visit(ASTunaryExpressionDef node, Object data) {
        if(node.firstToken.kind==CLang.SUB){
            _text.add("pop rax");
            _text.add("NEG rax");
            _text.add("push rax");
        }
        if(node.firstToken.kind==CLang.NOT){
            _text.add("pop rax");
            _text.add("NOT rax");
            _text.add("push rax");
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTfuncCall node, Object data) {

        func e = resolvFunc(node.firstToken.image); //check if func labal exist
        if(e==null){
            //error
            System.err.println(String.format("Function %s is not defined at %d : %d",
            node.firstToken.image,
            node.firstToken.beginLine,
            node.firstToken.beginColumn));
            System.exit(-1);
        }
        //System.err.println(String.format("Function %s is  defined ", node.firstToken.image));
        if(return_val!="void"){ //check if the func is call to assign var ,and make sure that the func dont return void
            if(e.type=="void"){
                //error
                System.err.println(String.format("Return value is unvalid at %d : %d",
                node.firstToken.beginLine,
                node.firstToken.beginColumn));
                System.exit(-1);
            }
        }
       
       curr = e;
        
        Object res =super.visit(node, data);
        if(curr.counter!=curr.arg){
            //error no enough arg
            System.err.println(String.format("Not enough arguments in Function %s at %d : %d",
            node.firstToken.image,
            node.firstToken.beginLine,
            node.firstToken.beginColumn));
            System.exit(-1);
        }
        _text.add(String.format("call %s", e.id));// go to func lable
        curr.counter=0;
        curr=null;
        return res;
    }

    @Override
    public Object visit(ASTaddExpressionDef node, Object data) {
        data = node.children[0].jjtAccept(this, data);
        if (node.children.length > 1)
        {
            
            data = node.children[1].jjtAccept(this, data);
            _text.add("pop rbx");
            _text.add("pop rax");
            if(node.firstToken.kind== CLang.ADD)
                _text.add("add rax, rbx");
            if(node.firstToken.kind== CLang.SUB)
                _text.add("sub rax, rbx");
            _text.add("push rax");
        }

        return data;
    }
    
    @Override
    public Object visit(ASTconstExpressionDef node, Object data) {
        
        if (node.firstToken.kind == CLang.ID)
        {
            //System.err.println(String.format("in conset"));
            SymbolTableEntry e = resolve(node.firstToken.image);
            if (e == null)
            {
                System.err.println(String.format("Variable %s is not defined at %d : %d",
                                                    node.firstToken.image,
                                                    node.firstToken.beginLine,
                                                    node.firstToken.beginColumn));
                System.exit(-1);
            }

            boolean isInt = e.type.equals("int");

            _text.add(String.format("mov %s, %s [rbp - %d]", isInt ? "eax" : "al", isInt ? "dword" : "byte", e.offset));
            _text.add("push rax");

        }

        if (node.firstToken.kind == CLang.NUMBER||node.firstToken.kind == CLang.CHAR_VALUE )
        {
            _text.add(String.format("mov rax, %s", node.firstToken.image));
            _text.add("push rax");
        }

        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTfunctionDef node, Object data) {
        func temp=resolvFunc(node.firstToken.next.image);
        if(temp!=null){
            System.err.println(String.format("Function %s is already defined %d : %d",
            node.firstToken.next.image,
            node.firstToken.beginLine,
            node.firstToken.beginColumn));
            System.exit(-1);
        }
        SymbolTableEntry temp2 = resolve(node.firstToken.next.image);
        if(temp2!=null){
            System.err.println(String.format("Function %s is already defined as variable %d : %d",
            node.firstToken.next.image,
            node.firstToken.beginLine,
            node.firstToken.beginColumn));
            System.exit(-1);
        }
        _text.add(String.format("%s:" ,node.firstToken.next.image));//this make a labal to the func
        _text.add("push rbp");
        _text.add("mov rbp, rsp");
        temp = new func(node.firstToken.next.image,node.firstToken.image);
        putFunc(temp);
        curr =temp;
      

     
        Object o = super.visit(node, data);
    
        return o;
        
    }

    @Override
    public Object visit(ASTVoidfunctionDef node, Object data) {     
        Object o = super.visit(node, data);
        _text.add("mov rsp, rbp");
        _text.add("pop rbp");
        _text.add("ret");
        return o;
        
    }

    @Override
    public Object visit(ASTreturn_val node, Object data) {     
        Object o = super.visit(node, data);
        _text.add("mov rsp, rbp");
        _text.add("pop rbp");
        _text.add("ret");
        return o;
        
    }

    @Override
    public Object visit(ASTVoidStatementDef node, Object data) { 
        boolean rightChild = false;  
        if(node.firstToken.image=="return"){
            rightChild=true;
        }  
        Object o = super.visit(node, data);
        if(rightChild==true){
        _text.add("mov rsp, rbp");
        _text.add("pop rbp");
        _text.add("ret");
        }
        
        return o;
        
    }



    @Override
    public Object visit(ASTVoidStatementBlockDef node, Object data) {
        index++;
        HashMap<String, SymbolTableEntry> temp = new HashMap<String, SymbolTableEntry>();
        symbols.add(index,temp);
        if(curr!=null){
            //System.err.println(String.format("curr = %s",curr.id));
            for(int t=0;t<curr.arg;t++){
                SymbolTableEntry e = curr.getParm(t);
                if(e!=null){
                    put(e);
                }
            }
            curr=null;
        }
        
        Object o = super.visit(node, data);
        symbols.remove(symbols.get(index));
        index--;
        return o;

    }

    @Override
    public Object visit(ASTStatementBlockDef node, Object data) {
        index++;
        HashMap<String, SymbolTableEntry> temp = new HashMap<String, SymbolTableEntry>();
        symbols.add(index,temp);             
        Object o = super.visit(node, data);
        symbols.remove(symbols.get(index));
        index--;
        return o;

    }
    @Override
    public Object visit(ASTfuncBlockdef node, Object data) {
        index++;
        HashMap<String, SymbolTableEntry> temp = new HashMap<String, SymbolTableEntry>();
        symbols.add(index,temp);
        if(curr!=null){
            //System.err.println(String.format("in block %d curr = %s",index,curr.id));
            for(int t=0;t<curr.arg;t++){
                  SymbolTableEntry e = curr.getParm(t);
                  if(e!=null){
                    put(e);
                    }
            }
            curr=null;
        }
        Object o = super.visit(node, data);
        symbols.remove(symbols.get(index));
        index--;
        return o;

    }

    @Override
    public Object visit(ASTassignExpressionDef node, Object data) {
        SymbolTableEntry e = resolve(node.firstToken.image);
        if (e == null)
        {
            System.err.println(String.format("Variable %s is not defined at %d : %d",
                                                node.firstToken.image,
                                                node.firstToken.beginLine,
                                                node.firstToken.beginColumn));
            System.exit(-1);
        }
        return_val = e.type;
        boolean isInt = e.type.equals("int");
        Object o = super.visit(node, data);
        _text.add("pop rax");
         _text.add(String.format("mov %s [rbp - %d], %s ",isInt ? "dword" : "byte", e.offset, isInt ? "eax" : "al"));
        _text.add("push rax");
        return_val="void";
        return o;
    }

    


    @Override
    public Object visit(ASTfuncExpressionDef node, Object data) {
        if(curr.counter>=curr.arg){
            System.err.println(String.format("Function %s has too many arguments %d : %d",
            curr.id,
            node.firstToken.beginLine,
            node.firstToken.beginColumn));
            System.exit(-1);
        }

        SymbolTableEntry  e = curr.getParm(curr.counter);
        return_val = e.type;
        boolean isInt = e.type.equals("int");
        Object o = super.visit(node, data);
        _text.add("pop rax");
        _text.add(String.format("mov %s [rbp - %d], %s ",isInt ? "dword" : "byte", e.offset, isInt ? "eax" : "al"));
        _text.add("push rax");
        return_val="void";
        curr.counter++;
        return o;
    }

    @Override
    public Object visit(ASTStart node, Object data) {

        Object o = super.visit(node, data);

        System.out.println("SECTION .TEXT\n" +
        "GLOBAL _start\n" +
        "_start:\n" +
        "call main\n" +
        "mov eax, 1\n" +
        "xor ebx, ebx\n" +
        "int 0x80\n" +
        "\n" +
        "printChar:\n" +
        "    push rbp\n" +
        "    mov rbp, rsp\n" +
        "    push rdi\n" +
        "    mov byte [rbp - 5], 0x41\n" +
        "    mov byte [rbp - 4], 0x53\n" +
        "    mov byte [rbp - 3], 0x41\n" +
        "    mov byte [rbp - 2], 0x46\n" +
        "    mov byte [rbp - 1], 0\n" +
        "    mov rax, 1\n" +
        "    mov rdi, 1\n" +
        "    lea rsi, [rbp -5]\n" +
        "    mov rdx, 5\n" +
        "    syscall \n" +
        "\n" +
        "    mov rsp, rbp\n" +
        "    pop rbp\n" +
        "    ret\n" +
        "\n" +
        "printNumber:\n" +
        "    push rbp\n" +
        "    mov rbp, rsp\n" +
        "    mov rsi, rdi\n" +
        "    lea rdi, [rbp - 1]\n" +
        "    mov byte [rdi], 0\n" +
        "    mov rax, rsi\n" +
        "    while:\n" +
        "    cmp rax, 0\n" +
        "    je done\n" +
        "    mov rcx, 10\n" +
        "    mov rdx, 0\n" +
        "    div rcx\n" +
        "    dec rdi\n" +
        "    add dl, 0x30\n" +
        "    mov byte [rdi], dl\n" +
        "    jmp while\n" +
        "\n" +
        "    done:\n" +
        "        mov rax, 1\n" +
        "        lea rsi, [rdi]\n" +
        "        mov rdx, rsp\n" +
        "        sub rdx, rsi\n" +
        "        mov rdi, 1\n" +
        "        syscall \n" +
        "\n" +
        "        mov rsp, rbp\n" +
        "        pop rbp\n" +
        "        ret\n" +
        "\n" +
        "readInteger:\n" +
        "    push rbp\n" +
        "    mov rbp, rsp\n" +
        "\n" +
        "    mov rdx, 10\n" +
        "    mov qword [rbp - 10], 0\n" +
        "    mov word [rbp - 2], 0\n" +
        "    lea rsi, [rbp - 10]\n" +
        "    mov rdi, 0 ; stdin\n" +
        "    mov rax, 0 ; sys_read\n" +
        "    syscall\n" +
        "\n" +
        "    xor rax, rax\n" +
        "    xor rbx, rbx\n" +
        "    lea rcx, [rbp - 10]\n" +
        "    \n" +
        "    copy_byte:\n" +
        "        cmp rbx, 10\n" +
        "        je read_done    \n" +
        "        mov dl, byte [rcx]\n" +
        "        cmp dl, 10\n" +
        "        jle read_done\n" +
        "        sub rdx, 0x30\n" +
        "        imul rax, 10\n" +
        "        add rax, rdx\n" +
        "        nextchar:\n" +
        "            inc rcx\n" +
        "            inc rbx\n" +
        "            jmp copy_byte\n" +
        "    read_done:\n" +
        "        mov rsp, rbp\n" +
        "        pop rbp\n" +
        "        ret\n" +
        "\n");
        for (String s : _text)
            System.out.println(s);
        return o;
    }

    @Override
    public Object visit(ASTWhileStatementDef node, Object data) {
        int labalDiffer=loopCounter;
        loopCounter++;
        _text.add(String.format("While%d:",labalDiffer));//this make a labal to the func
        data = node.children[0].jjtAccept(this, data); //condition
        _text.add("pop rax");
        _text.add("cmp rax,0");
        _text.add(String.format("jz EndWhile%d",labalDiffer));//jump to endwhile if not true
        //else
        data = node.children[1].jjtAccept(this, data); //statement or block
        _text.add(String.format("jmp While%d",labalDiffer));//jump to while 
        _text.add(String.format("EndWhile%d:",labalDiffer));//this make a labal to the end
        return data;
    }

    @Override
    public Object visit(ASTVoidWhileStatementDef node, Object data) {
        int labalDiffer=loopCounter;
        loopCounter++;
        _text.add(String.format("While%d:",labalDiffer));//this make a labal to the func
        data = node.children[0].jjtAccept(this, data); //condition
        _text.add("pop rax");
        _text.add("cmp rax,0");
        _text.add(String.format("jz EndWhile%d",labalDiffer));//jump to endwhile if not true
        //else
        data = node.children[1].jjtAccept(this, data); //statement or block
        _text.add(String.format("jmp While%d",labalDiffer));//jump to while 
        _text.add(String.format("EndWhile%d:",labalDiffer));//this make a labal to the end
        return data;
    }

    @Override
    public Object visit(ASTVoidForStatementDef node, Object data) {
        int labalDiffer=loopCounter;
        loopCounter++;
        int i=0;
        //int samiCounter=0;
        boolean isIndex = false;
        
        if(node.children[i].getClass().equals(ASTfirstForInit.class)){
            //index++;
            index++;
            HashMap<String, SymbolTableEntry> temp = new HashMap<String, SymbolTableEntry>();
            symbols.add(index,temp);
            isIndex=true;
            data = node.children[i].jjtAccept(this, data); //init always do
            i++;
            
        }
        _text.add(String.format("ForCondition%d:",labalDiffer));//this make a labal to the ForCondition    
        if(node.children[i].getClass().equals(ASTcondition.class) ){
            data = node.children[i].jjtAccept(this, data); //condition
            i++;
            _text.add("pop rax");
            _text.add("cmp rax,0");
            _text.add(String.format("jz EndFor%d",labalDiffer));//jump to endwhile if not true//jump to endfor if not true
        }
        
        _text.add(String.format("jmp ForBlock%d",labalDiffer));//else jump to ForBlock
        
        
        _text.add(String.format("ForEndInit%d:",labalDiffer));//this make a labal to the init end loop
        if(node.children[i].getClass().equals(ASTendForInit.class))
          {
            data = node.children[i].jjtAccept(this, data); //init end loop
            i++;
          }
        _text.add(String.format("jmp ForCondition%d",labalDiffer));//jump to condition
       
        _text.add(String.format("ForBlock%d:",labalDiffer));//this make a labal to the block
        data = node.children[i].jjtAccept(this, data); //statement or block
        _text.add(String.format("jmp ForEndInit%d",labalDiffer));//jump to ForEndInit 
       
          //--index
        if(isIndex==true){
            symbols.remove(symbols.get(index));
            index--;
        }

        _text.add(String.format("EndFor%d:",labalDiffer));//this make a labal to the end
        return data;
    }

    @Override
    public Object visit(ASTForStatementDef node, Object data) {
        int labalDiffer=loopCounter;
        loopCounter++;
        int i=0;
        //int samiCounter=0;
        boolean isIndex = false;
        
        if(node.children[i].getClass().equals(ASTfirstForInit.class)){
            //index++;
            index++;
            HashMap<String, SymbolTableEntry> temp = new HashMap<String, SymbolTableEntry>();
            symbols.add(index,temp);
            isIndex=true;
            data = node.children[i].jjtAccept(this, data); //init always do
            i++;
            
        }
        _text.add(String.format("ForCondition%d:",labalDiffer));//this make a labal to the ForCondition    
        if(node.children[i].getClass().equals(ASTcondition.class) ){
            data = node.children[i].jjtAccept(this, data); //condition
            i++;
            _text.add("pop rax");
            _text.add("cmp rax,0");
            _text.add(String.format("jz EndFor%d",labalDiffer));//jump to endwhile if not true//jump to endfor if not true
        }
        
        _text.add(String.format("jmp ForBlock%d",labalDiffer));//else jump to ForBlock
        
        
        _text.add(String.format("ForEndInit%d:",labalDiffer));//this make a labal to the init end loop
        if(node.children[i].getClass().equals(ASTendForInit.class))
          {
            data = node.children[i].jjtAccept(this, data); //init end loop
            i++;
          }
        _text.add(String.format("jmp ForCondition%d",labalDiffer));//jump to condition
       
        _text.add(String.format("ForBlock%d:",labalDiffer));//this make a labal to the block
        data = node.children[i].jjtAccept(this, data); //statement or block
        _text.add(String.format("jmp ForEndInit%d",labalDiffer));//jump to ForEndInit 
       
          //--index
        if(isIndex==true){
            symbols.remove(symbols.get(index));
            index--;
        }

        _text.add(String.format("EndFor%d:",labalDiffer));//this make a labal to the end
        return data;
    }

    @Override
    public Object visit(ASTVoidIfStatementDef node, Object data) {
        int labalDiffer=loopCounter;
        loopCounter++;
        _text.add(String.format("If%d:",labalDiffer));//this make a labal to the func
        data = node.children[0].jjtAccept(this, data); //condition
        _text.add("pop rax");
        _text.add("cmp rax,0");
        _text.add(String.format("jz EndIf%d",labalDiffer));//jump to endwhile if not true//jump to endIf if not true
        //else
        data = node.children[1].jjtAccept(this, data); //statement or block
        _text.add(String.format("EndIf%d:",labalDiffer));//this make a labal to the end
        return data;
    }

    @Override
    public Object visit(ASTIfStatementDef node, Object data) {
        int labalDiffer=loopCounter;
        loopCounter++;
        _text.add(String.format("If%d:",labalDiffer));//this make a labal to the func
        data = node.children[0].jjtAccept(this, data); //condition
        _text.add("pop rax");
        _text.add("cmp rax,0");
        _text.add(String.format("jz EndIf%d",labalDiffer));//jump to endwhile if not true//jump to endIf if not true
        //else
        data = node.children[1].jjtAccept(this, data); //statement or block
        _text.add(String.format("EndIf%d:",labalDiffer));//this make a labal to the end
        return data;
    }

    

    @Override
    public Object visit(ASTbinaryBoolExpressionCompareDef node, Object data) {
        
        data = node.children[0].jjtAccept(this, data);
        if (node.children.length > 1)
        {
            data = node.children[1].jjtAccept(this, data);
            
            
            _text.add("pop rbx");
            _text.add("pop rax");
            _text.add("cmp rax,rbx");  
            if(node.firstToken.kind==CLang.LT) //Of!=sf
            {
                _text.add("cmp OF , SF");// check if OF!=sf 
                _text.add("mov  rax ,ZF"); //save ZF
                _text.add("NOT rax"); //true if(zf==0)
            }

            if(node.firstToken.kind==CLang.LTE) // OF!=sf || zf==1
            {
                _text.add("mov rax,ZF"); //save zf from cmp1 in rax
                _text.add("cmp OF , SF");// check if OF!=sf 
                _text.add("mov rbx ,ZF"); 
                _text.add("not rbx"); // true if(OF!=sf)
                _text.add("or rax,rbx");//check if OF!=sf || ZF==1
            } 
                
            if(node.firstToken.kind==CLang.GT) //zf==0 && OF!=sf
            {
                _text.add("mov rax,ZF"); //save zf from cmp1 in rax
                _text.add("NOT rax"); //true if(zf==0)
                _text.add("cmp OF , SF");// check if OF==sf 
                _text.add("and rax ,ZF"); //check if OF==sf && ZF==1
            } 
            if(node.firstToken.kind==CLang.GTE)//zf==1 || OF!=sf
            {
                _text.add("mov rax,ZF"); //save zf from cmp1 in rax
                _text.add("cmp OF , SF");// check if OF==sf 
                _text.add("or rax ,ZF"); //check if OF==sf || ZF==1   
            }        
            
            _text.add("push rax");
        }

        return data;
    }

    @Override
    public Object visit(ASTbinaryExpressionEqDef node, Object data) {
        
        data = node.children[0].jjtAccept(this, data);
        if (node.children.length > 1)
        {
            data = node.children[1].jjtAccept(this, data);
            
            
            _text.add("pop rbx");
            _text.add("pop rax");
            _text.add("cmp rax,rbx");  
            _text.add("mov  rax ,ZF"); // save ZF
            if(node.firstToken.kind==CLang.NEQ) 
            {
                _text.add("NOT rax"); // true neq

            } 
            _text.add("push rax");
        }

        return data;
    }

    @Override
    public Object visit(ASTbinaryBoolExpressionOrDef node, Object data) {
        
        data = node.children[0].jjtAccept(this, data);
        if (node.children.length > 1)
        {
            data = node.children[1].jjtAccept(this, data);
            
            
            _text.add("pop rbx");
            _text.add("pop rax");
            _text.add("or rax,rbx");  
            _text.add("push rax");
        }

        return data;
    }

    @Override
    public Object visit(ASTbinaryBoolExpressionAndDef node, Object data) {
        
        data = node.children[0].jjtAccept(this, data);
        if (node.children.length > 1)
        {
            data = node.children[1].jjtAccept(this, data);
            
            
            _text.add("pop rbx");
            _text.add("pop rax");
            _text.add("and rax,rbx");  
            _text.add("push rax");
        }

        return data;
    }

    @Override
    public Object visit(ASTmulExpressionDef node, Object data) {
        //we need to sperate to mul, div, mod
        data = node.children[0].jjtAccept(this, data);
        if (node.children.length > 1)
        {
            data = node.children[1].jjtAccept(this, data);
            
            
                _text.add("pop rbx");
                _text.add("pop rax");
                if(node.firstToken.kind==CLang.DIV||node.firstToken.kind==CLang.MOD){
                _text.add("idiv rbx");   
                if(node.firstToken.kind==CLang.MOD)
                    _text.add("mov rax, ah");  
                }
                if(node.firstToken.kind==CLang.MUL)
                    _text.add("imul rax, rbx");
                if(node.firstToken.kind==CLang.MOD)
                    _text.add("imul rax, rbx");        
            
            _text.add("push rax");
        }

        return data;
    }


    
    public static void main(String[] args) throws FileNotFoundException, ParseException {
        new CLang(new FileInputStream(args[0]));

        CLang.Start();

        System.out.println("Parsing succeeded, begin compiling");
        

        CLang.jjtree.rootNode().jjtAccept(new SymbolTableVisitor(), null);
    }
}