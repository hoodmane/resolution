package res;

import java.util.Arrays;

public class Matrices
{
    private final int P,Q;
    private final ResMath resmath;
    public Matrices(int P){
        this.P = P;
        this.Q = 2*P-2;
        this.resmath = ResMath.getInstance(P);
    }
    /* row-reduces a matrix (in place).
     * Returns an array giving the column position of the leading 1 in each row. 
     * It should be noted that matrices are assumed to be reduced to lowest
     * non-negative residues (mod p), and this operation respects that. */
    public int[] rref(int[][] mat, int preserve_right)
    {
        if(mat.length == 0)
            return new int[] {};

        int h = mat.length;
        int w = mat[0].length;

        int good_rows = 0;
        int[] leading_cols = new int[h];
        for(int j = 0; j < w - preserve_right; j++) {
            /* find the first nonzero entry in this column */
            int i;
            for(i = good_rows; i < h && mat[i][j] == 0; i++);
            if(i == h) continue;

            /* swap the rows */
            int[] row = mat[good_rows];
            mat[good_rows] = mat[i];
            mat[i] = row;
            i = good_rows++;
            leading_cols[i] = j;

            /* normalize the row */
            int inv = resmath.inverse(mat[i][j]);
            for(int k = h; k < w; k++)
                mat[i][k] = (mat[i][k] * inv) % P;

            /* clear the rest of the column. this part is cubic-time so we optimize P=2 */
            if(P == 2) {
                for(int k = 0; k < h; k++) {
                    if(mat[k][j] == 0) continue;
                    if(k == i) continue;
                    for(int l = 0; l < w; l++)
                        mat[k][l] ^= mat[i][l];
                }
            } else {
                for(int k = 0; k < h; k++) {
                    if(mat[k][j] == 0) continue;
                    if(k == i) continue;
                    int mul = P - mat[k][j];
                    for(int l = 0; l < w; l++)
                        mat[k][l] = (mat[k][l] + mat[i][l] * mul) % P;
                }
            }
        }

        return Arrays.copyOf(leading_cols, good_rows);
    }


    /* unused */
    public void printMatrix(String name, int[][] mat)
    {
        System.out.print(name + ":");
        if(mat.length == 0) {
            System.out.println(" <zero lines>");
            return;
        }

        for (int[] mat1 : mat) {
            System.out.println();
            for (int j = 0; j < mat[0].length; j++) {
                System.out.printf("%2d ", mat1[j]);
            }
        }
        System.out.println();
    }

    public double[] transform3(double[][] m, double[] v)
    {
        return new double[] {
            m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
            m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]
        };
    }

    public double[][] mmult3(double[][] m, double[][] n)
    {
        double[][] r = new double[3][3];
        for(int i = 0; i < 3; i++)
            for(int j = 0; j < 3; j++)
                for(int k = 0; k < 3; k++)
                    r[i][k] += m[i][j] * n[j][k];
        return r;
    }

    public double[][] transpose3(double[][] m) {
        double[][] r = new double[3][3];
        for(int i = 0; i < 3; i++)
            for(int j = 0; j < 3; j++)
                r[i][j] = m[j][i];
        return r;
    }
}
