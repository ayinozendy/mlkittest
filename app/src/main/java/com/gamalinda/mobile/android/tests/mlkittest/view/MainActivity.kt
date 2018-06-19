package com.gamalinda.mobile.android.tests.mlkittest.view

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.gamalinda.mobile.android.tests.mlkittest.R
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }

    lateinit var prediction: TextView
    lateinit var confidence: TextView
    lateinit var pictureView: ImageView
    lateinit var photoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_base_layout)

        prediction = findViewById(R.id.prediction)
        confidence = findViewById(R.id.confidence)
        pictureView = findViewById(R.id.pictureView)
        val button = findViewById<Button>(R.id.takePictureButton)
        button.setOnClickListener { takePicture() }
    }

    private fun takePicture() {
        val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        pictureIntent.resolveActivity(packageManager)?.let {
            val photoFile = createImageFile()
            photoFile.absolutePath
            val photoURI = FileProvider.getUriForFile(this,
                    "com.gamalinda.mobile.android.tests.mlkittest.fileprovider",
                    photoFile.canonicalFile)
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(pictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = filesDir
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        photoPath = image.absolutePath
        return image
    }

    private fun getPic(): Bitmap {
        // Get the dimensions of the View
        val targetW = pictureView.getWidth()
        val targetH = pictureView.getHeight()

        val ei = ExifInterface(photoPath)
        val exifResult = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        // Get the dimensions of the bitmap
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true

        BitmapFactory.decodeFile(photoPath, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight

        // Determine how much to scale down the image
        val scaleFactor = Math.min(photoW / targetW, photoH / targetH)

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inPurgeable = true

        var bitmap = BitmapFactory.decodeFile(photoPath, bmOptions)

        bitmap = rotateImage(bitmap, exifResult)
        pictureView.setImageBitmap(bitmap)

        return bitmap
    }

    private fun rotateImage(bm: Bitmap, exifResult: Int): Bitmap {
        val rotation = when (exifResult) {
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                ORIENTATIONS.get(270)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                ORIENTATIONS.get(180)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                ORIENTATIONS.get(0)
            }
            else -> {
                ORIENTATIONS.get(90)
            }
        }
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        val rotatedImg = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
        if (rotatedImg != bm) {
            bm.recycle()
        }
        return rotatedImg
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val bitmap = getPic()
            pictureView.setImageBitmap(bitmap)
            doTensorFlowImageClassify(bitmap)
        }
    }

    private fun doTensorFlowImageClassify(bitmapImage: Bitmap) {
        val fvi = FirebaseVisionImage.fromBitmap(bitmapImage)
        val detector: FirebaseVisionTextDetector = FirebaseVision.getInstance().visionTextDetector
        val result = detector.detectInImage(fvi)
                .addOnSuccessListener {
                    // Task completed successfully
                    // ...
                    doFirebaseSuccess(it)
                }
                .addOnFailureListener {
                    // Task failed with an exception
                    // ...
                }
    }

    private fun doFirebaseSuccess(fvt: FirebaseVisionText) {
        for (block in fvt.getBlocks()) {
            val boundingBox = block.getBoundingBox()
            val cornerPoints = block.getCornerPoints()
            val text = block.getText()

            for (line in block.getLines()) {
                // ...
                for (element in line.getElements()) {
                    // ...
                }
                prediction.text = line.text
            }
        }
    }
}
