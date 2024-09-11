package vn.bvntp.app.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import vn.bvntp.app.App
import vn.bvntp.app.databinding.FragmentHoSoBenhAnBinding
import vn.bvntp.app.ui.activity.PdfViewer
import vn.bvntp.app.viewmodel.HoSoBenhAnViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HoSoBenhAnFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HoSoBenhAnFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private var _binding: FragmentHoSoBenhAnBinding ? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHoSoBenhAnBinding.inflate(inflater, container, false)
        val view = binding.root

        // Inflate the layout for this fragment
        val appContainer = (requireActivity().applicationContext as App).container
        val hsbaViewModel = ViewModelProvider(requireActivity(), appContainer.hsbaViewModelFactory).get(
            HoSoBenhAnViewModel::class.java)

        binding.hsbaViewModel = hsbaViewModel

        val context = requireContext()
        hsbaViewModel.modelLichSuVaoVienView(
            context
        ) {
        }

        hsbaViewModel.isLock.observe(viewLifecycleOwner, Observer { isLock ->
            // Update UI, e.g., show or hide a progress bar
            if (isLock) {
                // Show progress bar
                binding.loadingIcon.visibility = View.VISIBLE
                binding.textHide.visibility = View.INVISIBLE
            } else {
                // Hide progress bar
                binding.textHide.visibility = View.VISIBLE
                binding.loadingIcon.visibility = View.GONE
            }
        })

        hsbaViewModel.maVaoVien.observe(viewLifecycleOwner, Observer {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Đang xử lý mã vào viện: " + hsbaViewModel.maVaoVien.value, Toast.LENGTH_SHORT).show()
            }
            hsbaViewModel.modelHoSoBenhAnView(context) {
                val intent = Intent(
                    context, PdfViewer::class.java
                )
                intent.putExtra(
                    "fileUri", hsbaViewModel.getTemp()
                )
                context.startActivity(intent)
            }
        })




        return view

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HoSoBenhAnFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HoSoBenhAnFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}