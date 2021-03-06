#This snippet is just for showing that I know MIPS
# middle = (start + end ) / 2
    add $t0, $s0, $s2                   # $t0 = start + end
    sra  $s1, $t0, 1                    # $s1 = $t0 / 2

    # save $ra in the stack
    addi $sp, $sp, -4
    sw $ra, 0($sp)

    # base case
    ble $s2, $s0, returnNegative1       # if (end <= start) 

    lw  $t1, arr($s1)                   # $t1 = arr[middle]
    lw  $t2, val                        # $t2 = val
    beq $t1, $t2, returnMiddle          # if (arr[middle] == val)

    blt $t2, $t1, returnFirstPart       # if (val < arr[middle])

    bgt $t2, $t1, returnLastPart        # if (val > arr[middle])  

    returnNegative1:
       li $v0, -1
       j Exit       
    returnMiddle:
       move $v0, $s1                    # return middle
       j Exit   

    returnFirstPart:
           move $t3, $s1                # temp = middle     
           addi $t3, $t3, -1            # temp --
           move $s2, $t3                # end = temp
           jal BinarySearch
       j Exit

    returnLastPart:                             
       move $t3, $s1                    # temp = middle
       addi $t3, $t3, 1                 # temp++
       move $s0, $t3                    # start = temp
       jal BinarySearch
       j Exit